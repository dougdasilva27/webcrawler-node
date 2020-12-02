package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.AdvancedRatingReview
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.Installment
import models.pricing.Installments
import models.pricing.Pricing
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.util.*
import kotlin.math.roundToInt

class BrasilEtnamoveisCrawler(session: Session?) : Crawler(session) {

   private val homePage = "https://www.etna.com.br"
   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith(homePage)
   }

   val scrapedVariations = mutableListOf<String>()
   var productsLength = 0

   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products = mutableListOf<Product>()
      if (doc.selectFirst("#js-page-details-container") != null) {
         val images = doc.select(".zoomIt img").map { homePage + it.attr("data-zoom-image") }
         val offers: Offers = scrapOffers(doc)
         products += CrawlerUtils.scrapSchemaOrg(doc)
            .setPrimaryImage(images.first())
            .setUrl(session.originalURL)
            .setCategories(doc.select(".breadcrumb li").eachText(arrayOf(0)))
            .setSecondaryImages(images.sliceFirst())
            .setOffers(offers)
            .setRatingReviews(scrapRatingReviews())
            .build()

         val variations = doc.select(".form-control.etn-select--custom.variant-select option")?.sliceFirst()
         if (variations != null) {
            scrapedVariations addNonNull doc.selectFirst("#currentSizeValue")?.attr("data-size-value")
            if (productsLength == 5) return products
            variations
               .filter { it.text().trim() !in scrapedVariations }
               .forEach {
                  val body = dataFetcher.get(session, Request.RequestBuilder.create().setUrl(homePage + it.attr("value")!!).build()).body?.toDoc()
                  if (body != null) {
                     productsLength++
                     products += extractInformation(body)
                  }
               }
         }
      }

      return products
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val price = doc.selectFirst(".etn-price__list")?.toDoubleComma()
      var priceFrom: Double? = null
      var installments = Installments()
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build())
      if(doc.selectFirst("etn-price__old") != null){
         priceFrom = doc.selectFirst("etn-price__old")?.toDoubleComma()
      }
      if(doc.selectFirst(".easy-installment") != null){
         installments = Installments(setOf(doc.installment(".easy-installment")))
      }
      val pricing = Pricing.PricingBuilder
         .create()
         .setSpotlightPrice(price)
         .setCreditCards(listOf(MASTERCARD, VISA, AMEX, ELO, HIPERCARD, DINERS).toCreditCards(installments))
         .setBankSlip(price?.toBankSlip())
         .setPriceFrom(priceFrom).build()
      val sellerName = doc.selectFirst(".etn-product__delivery strong")?.text()
      offers.add(
         Offer.OfferBuilder
            .create()
            .setPricing(pricing)
            .setSellerFullName(sellerName)
            .setUseSlugNameAsInternalSellerId(true)
            .setIsBuybox(false)
            .setIsMainRetailer(sellerName?.matches("Etna".toRegex()) ?: true)
            .build()
      )
      return offers
   }

   private fun fetchRatings(): Document{

      val headers = HashMap<String, String>()
      headers["Content-Type"] = "text/html;charset=UTF-8"
      var url = session.originalURL

      if(session.originalURL.contains("?")){
         url = url.substring(0,session.originalURL.indexOf("?prod_list"))
      }

      val request = Request.RequestBuilder.create().setUrl(url + "/reviewhtml/3").setHeaders(headers).build()

      return Jsoup.parse(dataFetcher[session, request].body)
   }

   private fun scrapRatingReviews(): RatingsReviews{
      val ratingReviews = RatingsReviews()
      val page = fetchRatings()

      val numberOfRatingsReviews = page.select("li.review-entry").size
      val reviews = page.select(".rating-stars")
      var totalValueReviews = 0.0
      var avgRatingReviews = 0.0
      for(review in reviews){

         val ratingInfo: JSONObject = CrawlerUtils.stringToJSONObject(review.attr("data-rating"))

         val ratingValue = ratingInfo.optString("rating").toDouble().roundToInt()

         totalValueReviews += ratingValue
      }
      if(numberOfRatingsReviews != 0){
         avgRatingReviews  = totalValueReviews / numberOfRatingsReviews
      }

      ratingReviews.setTotalRating(numberOfRatingsReviews)
      ratingReviews.totalWrittenReviews = numberOfRatingsReviews
      ratingReviews.averageOverallRating = avgRatingReviews
      ratingReviews.advancedRatingReview = scrapAdvancedRating(reviews)

      return ratingReviews

   }

   private fun scrapAdvancedRating(reviews: Elements): AdvancedRatingReview{
      val advancedRatingReview = AdvancedRatingReview()
      var stars1 = 0
      var stars2 = 0
      var stars3 = 0
      var stars4 = 0
      var stars5 = 0

      for(e in reviews){

         val ratingInfo: JSONObject = CrawlerUtils.stringToJSONObject(e.attr("data-rating"))

         val ratingValue = ratingInfo.optString("rating").toDouble().roundToInt()
         when(ratingValue){
            1 -> {
               stars1++
               advancedRatingReview.totalStar1 = stars1
            }
            2 -> {
               stars2++
               advancedRatingReview.totalStar2 = stars2
            }
            3 -> {
               stars3++
               advancedRatingReview.totalStar3 = stars3
            }
            4 -> {
               stars4++
               advancedRatingReview.totalStar4 = stars4
            }
            5 -> {
               stars5++
               advancedRatingReview.totalStar5 = stars5
            }
         }
      }

      return advancedRatingReview
   }
}
