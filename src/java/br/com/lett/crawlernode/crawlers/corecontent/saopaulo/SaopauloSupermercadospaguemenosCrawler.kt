package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.AdvancedRatingReview
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class SaopauloSupermercadospaguemenosCrawler(session: Session?) : Crawler(session) {

   override fun fetch(): Document {
      val request = Request.RequestBuilder.create()
         .setUrl(session.originalURL)
         .setFollowRedirects(false)
         .build()

      return dataFetcher.get(session, request).body.toDoc() ?: Document(session.originalURL)
   }

   override fun extractInformation(document: Document): MutableList<Product> {
      val products = mutableListOf<Product>()

      if (isProductPage(document)) {

         val name = document.selectFirst("h1")?.text()
         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".bt-checkout", "data-sku")
         val categories = document.select("li a[itemprop='item'] span").eachText(ignoreIndexes = arrayOf(0))
         val available = isAvailable(document)
         val offers = if (available) scrapOffers(document) else Offers()

         val primaryImage = document.selectFirst(".cloud-zoom")?.attr("href")?.replaceFirst("//", "https://")
         val ratingReviews: RatingsReviews = scrapRating(document)


         // Creating the product
         products += ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setRatingReviews(ratingReviews)
            .build()
      }

      return products
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst("#product") != null
   }

   private fun isAvailable(doc: Document): Boolean{
      return scrapPrice(doc) != null
   }

   private fun scrapPrice(doc: Document): Double? {
     var price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sale .sale_price", null, true, ',', session)
      if ( price == 0.0) {
         price = null
      }
      return price
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val price = scrapPrice(doc)

      var priceFrom = doc.selectFirst(".box-pricing .price_off .unit_price")?.toDoubleComma()

      price?.let {
         if (it == priceFrom) {
            priceFrom = null
         }

         val creditCards = setOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX).toCreditCards(it)

         offers.add(
            OfferBuilder.create()
               .setPricing(
                  PricingBuilder.create()
                     .setCreditCards(creditCards)
                     .setSpotlightPrice(scrapPrice(doc))
                     .setPriceFrom(priceFrom)
                     .build()
               )
               .setIsMainRetailer(true)
               .setIsBuybox(false)
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName("Super Pague Menos")
               .build()
         )
      }
      return offers
   }

   private fun scrapRating(doc: Document): RatingsReviews {
      val ratingReviews = RatingsReviews()

      ratingReviews.date = session.date
      ratingReviews.setTotalRating(scrapTotalRating(doc))
      ratingReviews.averageOverallRating = scrapOverallRating(doc)
      ratingReviews.totalWrittenReviews = scrapTotalWrittenReviews(doc)
      ratingReviews.advancedRatingReview = scrapAdvancedTotalReviews(doc)

      return ratingReviews
   }

   private fun scrapTotalRating(doc: Document): Int {
      val ratingReviewsElements: Elements = doc.select("div#ratings .ratings-item")
      return ratingReviewsElements.size
   }

   private fun scrapOverallRating(doc: Document): Double {
      val totalRating = scrapTotalRating(doc).toDouble()
      var starsCount = 0.0

      for (ratingReviewElement: Element in doc.select("div#ratings .ratings-item")) {
         var ratingText: Element? = ratingReviewElement.selectFirst("p.rating-star>span")

         if (ratingText != null) {
            val stars: Double = ratingText.ownText().trim().toDouble()
            starsCount += stars
         }
      }

      var response = 0.0
      if (starsCount > 0 && totalRating > 0) {
         response = starsCount / totalRating
      }

      return response
   }

   private fun scrapTotalWrittenReviews(doc: Document): Int {
      var count = 0

      for (ratingReviewElement: Element in doc.select("div#ratings .ratings-item")) {
         val ratingText: Element? = ratingReviewElement.selectFirst("p.rating-opinion")
         if (ratingText != null && ratingText.ownText().trim().isNotEmpty())
            count++
      }

      return count
   }

   private fun scrapAdvancedTotalReviews(doc: Document): AdvancedRatingReview {
      val advancedRatingReview = AdvancedRatingReview.Builder()
      val starsCount = mutableMapOf<Int, Int>()

      for (ratingReviewElement: Element in doc.select("div#ratings .ratings-item")) {
         val ratingText: Element? = ratingReviewElement.selectFirst("p.rating-star>span")
         if (ratingText != null) {
            val star = ratingText.ownText().trim().toInt()
            val count = starsCount.getOrDefault(star, 0) + 1
            starsCount[star] = count
         }
      }

      return advancedRatingReview.allStars(starsCount).build()
   }

}
