package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Parser
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.round
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import models.AdvancedRatingReview
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class SaopauloPanvelCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.APACHE
      config.parser = Parser.JSON
   }

   private val uf: String = session.options.optString("uf")

   override fun fetchResponse(): Response {

      val headers: MutableMap<String, String> = HashMap()
      headers["app-token"] = "ZYkPuDaVJEiD"
      headers["authority"] = "www.panvel.com"
      headers["cookie"] = "appName=item;"

      val internalId = URL(session.originalURL).path.substringAfterLast("-")

      val url = "https://www.panvel.com/api/v2/catalog/$internalId?uf=$uf"

      val request = Request.RequestBuilder
         .create()
         .setHeaders(headers)
         .setUrl(url)
         .setProxyservice(listOf(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.LUMINATI_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build()

      return dataFetcher[session, request]
   }

   override fun extractInformation(json: JSONObject): List<Product> {
      val products: MutableList<Product> = ArrayList()
      if (json != null && !json.isEmpty) {
         val internalId = URL(session.originalURL).path.substringAfterLast("-")

         val categories = if (json.optJSONArray("categories") != null) json.optJSONArray("categories").map { (it as JSONObject).optString("description") } else emptyList()
         val jsonImages = json.optJSONArray("images").sortedBy { (it as JSONObject).optInt("number") }.toMutableList()
         val primaryImage = (jsonImages.removeFirst() as JSONObject).optString("url")
         val secondaryImages = jsonImages.map { (it as JSONObject).optString("url") }
         val name = json.optString("name")
         val productUrl = getProductUrl(session.originalURL.replace("'", "&apos;"))
         val isAvailable = json.optString("stockStatus", "").contains("InStock")
         val offers = if (isAvailable) scrapOffers(json) else Offers()
         val rating = crawlRating(json)

         val product = ProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setDescription(json.optString("description"))
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setRatingReviews(rating)
            .build()
         products.add(product)
      }

      return products
   }

   private fun crawlRating(json: JSONObject): RatingsReviews? {
      val ratingsReviews = RatingsReviews()

      val reviews = json.optJSONObject("itemReviews")
      if (reviews != null) {
         val rating = reviews.optString("averageRating")
         ratingsReviews.averageOverallRating = getAverageRating(rating)
         val totalReviews = reviews.optJSONArray("reviews")
         if (totalReviews != null) {
            ratingsReviews.advancedRatingReview = scrapAdvancedRatingReview(totalReviews)
            ratingsReviews.totalWrittenReviews = totalReviews.length()
         }
      }

      return ratingsReviews

   }

   private fun getAverageRating(rating: String): Double? {
      return when (rating) {
         "ONE_STARS" -> 1.0
         "TWO_STARS" -> 2.0
         "THREE_STARS" -> 3.0
         "FOUR_STARS" -> 4.0
         "FIVE_STARS" -> 5.0
         else -> null
      }

   }

   private fun scrapAdvancedRatingReview(reviews: JSONArray): AdvancedRatingReview? {
      var star1: Int? = 0
      var star2: Int? = 0
      var star3: Int? = 0
      var star4: Int? = 0
      var star5: Int? = 0
      for (review in reviews) {
         if (review is JSONObject) {
            when (review.optInt("reviewRating", 0)) {
               5 -> star5 = star5?.plus(1)
               4 -> star4 = star4?.plus(1)
               3 -> star3 = star3?.plus(1)
               2 -> star2 = star2?.plus(1)
               1 -> star1 = star1?.plus(1)
               else -> {}
            }
         }
      }
      return AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build()
   }

   private fun getProductUrl(url: String): String? {
      return if (countOccurrences(url) > 1) {
         url.replaceFirst("https://www.panvel.com".toRegex(), "")
      } else url
   }

   private fun countOccurrences(url: String): Int {
      val parts = url.split("https://www.panvel.com".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      return parts.size - 1
   }


   private fun scrapOffers(json: JSONObject): Offers {
      var price = json.optDouble("originalPrice")
      var priceFrom: Double? = null
      val discount = (json.optQuery("/discount/discountPercentage") as Int?)
      if (discount != 0 && discount != null) {
         price = json.optQuery("/discount/dealPrice") as Double
         priceFrom = json.optDouble("originalPrice")
      }
      val bankSlip = price.toBankSlip()
      val creditCards = listOf(Card.HIPERCARD, Card.VISA, Card.MASTERCARD, Card.AMEX, Card.DINERS).toCreditCards(price)
      val pricing = Pricing.PricingBuilder.create()
         .setSpotlightPrice(price.round())
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build()
      val sales = scrapSales(pricing, json)
      val offer = Offer.OfferBuilder.create()
         .setPricing(pricing)
         .setUseSlugNameAsInternalSellerId(true)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setSellerFullName("Panvel")
         .build()

      return Offers(listOf(offer))
   }

   private fun scrapSales(pricing: Pricing, json: JSONObject): MutableList<String>? {
      val sales = mutableListOf<String>()
      val discount = CrawlerUtils.calculateSales(pricing)
      if (discount != null && discount.isNotEmpty()) {
         sales.add(discount)
      }

      val descriptionSales = (json.optQuery("/pack/description") as String?)
      if (!descriptionSales.isNullOrEmpty()) {
         sales.add(descriptionSales)
      }

      return sales
   }


}
