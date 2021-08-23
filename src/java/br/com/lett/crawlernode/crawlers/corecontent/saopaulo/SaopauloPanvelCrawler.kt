package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.models.RequestMethod
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.time.LocalDate

class SaopauloPanvelCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.APACHE
      cookies.add(BasicClientCookie("stc112189", LocalDate.now().toEpochDay().toString()))
      cacheConfig.request = Request.RequestBuilder.create().setCookies(cookies).setUrl("https://www.panvel.com/panvel/main.do").build()
      cacheConfig.requestMethod = RequestMethod.GET
   }

   override fun fetch(): Any? {
      val request = Request.RequestBuilder.create().setCookies(cookies).setUrl(session.originalURL).build()

      val response = dataFetcher[session, request]
      return Jsoup.parse(response.body)
   }

   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products: MutableList<Product> = ArrayList()
      if (isProductPage(doc) && """.*p-\d*""".toRegex().matches(session.originalURL)) {
         val internalId = URL(session.originalURL).path.substringAfterLast("-")

         val json = unescapeHtml(doc.selectFirst("#serverApp-state").data()).toJson()
            .optJSONObject("api/v1/item/$internalId")

         val categories = json.optJSONArray("categories").map { (it as JSONObject).optString("description") }
         val jsonImages = json.optJSONArray("images").sortedBy { (it as JSONObject).optInt("number") }.toMutableList()
         val primaryImage = (jsonImages.removeFirst() as JSONObject).optString("url")
         val secondaryImages = jsonImages.map { (it as JSONObject).optString("url") }
         val name = json.optString("name")
         val isAvailable = doc.select(".text-unavailable-item").isEmpty()
         val offers = if (isAvailable) scrapOffers(json) else Offers()
         val rating = scrapRating(doc)

         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
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

   private fun isProductPage(doc: Document): Boolean {
      return doc.select(".produto-nome").first() != null
   }


   private fun unescapeHtml(str: String): String {
      return str.replace("&a;Ccedil;", "Ç")
         .replace("&a;ccedil;", "ç")
         .replace("&a;Aacute;", "Á")
         .replace("&a;Acirc;", "Â")
         .replace("&a;Atilde;", "Ã")
         .replace("&a;Eacute;", "É")
         .replace("&a;Ecirc;", "Ê")
         .replace("&a;Iacute;", "Í")
         .replace("&a;Ocirc;", "Ô")
         .replace("&a;Otilde;", "Õ")
         .replace("&a;Oacute;", "Ó")
         .replace("&a;Uacute;", "Ú")
         .replace("&a;aacute;", "á")
         .replace("&a;acirc;", "â")
         .replace("&a;atilde;", "ã")
         .replace("&a;eacute;", "é")
         .replace("&a;ecirc;", "ê")
         .replace("&a;iacute;", "í")
         .replace("&a;ocirc;", "ô")
         .replace("&a;otilde;", "õ")
         .replace("&a;oacute;", "ó")
         .replace("&a;uacute;", "ú")
         .replace("&a;nbsp;", " ")
         .replace("&q;", "\"")
         .replace("&s;", "'")
         .replace("&g;", ">")
         .replace("&l;", "<")
         .replace("&a;", "&")
         .replace("&a;reg;", "®")
   }

   private fun scrapOffers(json: JSONObject): Offers {
      var price = json.optDouble("originalPrice")
      var priceFrom: Double? = null
      val discount = (json.optQuery("/discount/percentage") as Int?)
      if (discount != 0 && discount != null) {
         price = json.optQuery("/discount/dealPrice") as Double
         priceFrom = json.optDouble("originalPrice")
      }
      val bankSlip = price.toBankSlip()
      val creditCards = listOf(Card.HIPERCARD, Card.VISA, Card.MASTERCARD, Card.AMEX, Card.DINERS).toCreditCards(price)

      val offer = Offer.OfferBuilder.create()
         .setPricing(
            Pricing.PricingBuilder.create()
               .setSpotlightPrice(price.round())
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .setBankSlip(bankSlip)
               .build()
         )
         .setUseSlugNameAsInternalSellerId(true)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSellerFullName("Panvel")
         .build()

      return Offers(listOf(offer))
   }

   private fun scrapRating(doc: Document): RatingsReviews {
      var rating = RatingsReviews()


      val s = doc.select(".rating .material-icons.star-on").size

      val average = MathUtils.parseDoubleWithComma(s.toString())

      var count = CrawlerUtils.scrapIntegerFromHtml(doc, ".rating span:not(.material-icons)", false, 0);

      if (average == 0.0) {
         count = 0
      }

      rating.setAverageOverallRating(average)
      rating.setTotalRating(count);
      rating.setTotalWrittenReviews(count);


      return rating;
   }
}
