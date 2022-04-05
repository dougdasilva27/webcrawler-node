package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import com.google.common.collect.Sets
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
import java.util.HashMap
import kotlin.math.roundToInt

class BrasilWinecrawler(session: Session?) : Crawler(session) {

   companion object {
      private const val SELLER_FULL_NAME = "Wine"
   }

   protected var internalId: String = "";

   protected var cards: Set<Card> = Sets.newHashSet(Card.VISA, Card.MASTERCARD,
      Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS)

   @Throws(Exception::class)
   override fun extractInformation(doc: Document): List<Product> {

      val products = mutableListOf<Product>()

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session,
            "Product page identified: " + session.originalURL)

         val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".PageHeader-title", false)

         val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a", false)

         internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[sku-code]", "sku-code")

         val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".js-product-img", "src")

         val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".ReadMore-text", ".TechnicalDetails"))

         val jsonOffer = CrawlerUtils.stringToJson(doc.selectFirst("price-box[campaigns]")?.attr(":product"))

         val isAvailable = jsonOffer.optBoolean("available", false)

         val offers = if (isAvailable) scrapOffers(jsonOffer) else Offers()

         val product = ProductBuilder()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(scrapRatingReviews())
            .build()

         products.add(product)
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }
      return products
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".ProductPage") != null
   }

   private fun scrapOffers(jsonOffer: JSONObject): Offers {
      val offers = Offers()

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(scrapPricing(jsonOffer))
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setSales(listOf())
            .build()
      )

      return offers
   }

   private fun scrapPricing(jsonOffer: JSONObject): Pricing {

      val price: Double = jsonOffer.optDouble("salePrice")

      val bankSlip = price.toBankSlip()

      val installments = Installments()

      installments.add(
         Installment.InstallmentBuilder()
            .setInstallmentNumber(1)
            .setInstallmentPrice(price)
            .build()
      )


      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS).toCreditCards(installments)

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build()
   }

   private fun scrapRatingReviews(): RatingsReviews {
      val ratingReviews = RatingsReviews()
      val page = fetchRatings()

      val numberOfRatingsReviews = CrawlerUtils.scrapIntegerFromHtml(page, "numberOfRatings", false, 0)

      var avgRatingReviews = 0.0

      if (numberOfRatingsReviews != 0) {
         avgRatingReviews = CrawlerUtils.scrapDoublePriceFromHtml(page, "averageRating", null, false, '.', session)
      }

      ratingReviews.setTotalRating(numberOfRatingsReviews)
      ratingReviews.totalWrittenReviews = numberOfRatingsReviews
      ratingReviews.averageOverallRating = avgRatingReviews
      ratingReviews.advancedRatingReview = scrapAdvancedRating(page)

      return ratingReviews

   }

   private fun fetchRatings(): Document {

      val headers = HashMap<String, String>()
      headers["Content-Type"] = "text/html;charset=UTF-8"

      val request = Request.RequestBuilder.create().setUrl("https://www.wine.com.br/api/v2/products/" + internalId + "?expand=attributes").setHeaders(headers).build()

      return Jsoup.parse(dataFetcher[session, request].body)
   }


   private fun scrapAdvancedRating(page: Document): AdvancedRatingReview {
      val advancedRatingReview = AdvancedRatingReview()
      var stars1 = 0
      var stars2 = 0
      var stars3 = 0
      var stars4 = 0
      var stars5 = 0
      var reviews = page.select("histogram entry")
      for (e in reviews) {


         val ratingValue = CrawlerUtils.scrapDoublePriceFromHtml(e, "string", null, false, '.', session)
         when (ratingValue) {
            1.0 -> {
               advancedRatingReview.totalStar1 = CrawlerUtils.scrapIntegerFromHtml(e, "long", false, 0)
            }
            2.0 -> {
               advancedRatingReview.totalStar2 = CrawlerUtils.scrapIntegerFromHtml(e, "long", false, 0)
            }
            3.0 -> {
               advancedRatingReview.totalStar3 = CrawlerUtils.scrapIntegerFromHtml(e, "long", false, 0)
            }
            4.0 -> {
               advancedRatingReview.totalStar4 = CrawlerUtils.scrapIntegerFromHtml(e, "long", false, 0)
            }
            5.0 -> {
               advancedRatingReview.totalStar5 = CrawlerUtils.scrapIntegerFromHtml(e, "long", false, 0)
            }
         }
      }

      return advancedRatingReview
   }

}
