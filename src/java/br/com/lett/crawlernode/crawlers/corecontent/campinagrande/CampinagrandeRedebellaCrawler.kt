package br.com.lett.crawlernode.crawlers.corecontent.campinagrande

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricingException
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing
import models.pricing.Pricing.PricingBuilder
import org.json.JSONObject
import org.jsoup.nodes.Document
import br.com.lett.crawlernode.test.Test
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher

class CampinagrandeRedebellaCrawler(session: Session) : Crawler(session) {

   private val BASE_URL: String = "redebella.com.br"
   private val SELLER_FULL_NAME: String = "Redebella"
   private val cards = listOf(
      Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString()
   )

   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)
      val products = mutableListOf<Product>()

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: ${session.originalURL}")

         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".quantity-control > input[name=\"product_id\"]", "value")
         val internalPid = internalId
         val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".title-product > h1", true)
         val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li", true)
         val description = CrawlerUtils.scrapSimpleDescription(doc, listOf("#tab-description"))
         val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".image-additional > a", listOf("data-image"), "https", BASE_URL)
         val secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".image-additional > a", listOf("data-image"), "https", BASE_URL, primaryImage)
         val availability = doc.selectFirst("#button-cart") != null
         val offers = if (availability) scrapOffers(doc, internalId) else Offers()
         val ratingsReviews = RatingsReviews()

         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(ratingsReviews)
            .build()

         products.add(product)

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }
      return products
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst("#product") != null
   }

   private fun scrapOffers(doc: Document, internalId: String): Offers {
      val offers = Offers()
      val pricing: Pricing = scrapPricing(internalId)
      val sales = mutableListOf<String>()
      sales addNonNull CrawlerUtils.scrapStringSimpleInfo(doc, ".label-sale", true)
      offers.add(
         OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build()
      )
      return offers
   }

   private fun scrapPricing(internalId: String): Pricing {
      val json = fetchPrices(internalId)
      val prices = json.optJSONObject("new_price")

      var spotlightPrice: Double? = null;
      var priceFrom: Double? = null;
      if (prices != null) {
         if (prices.optString("special") != "false") {
            spotlightPrice = prices.optString("special").toDoubleComma()
            prices.optString("price").toDoubleComma().also { priceFrom = it }
         } else {
            spotlightPrice = prices.optString("price").toDoubleComma()
            priceFrom = null
         }
      }

      spotlightPrice?.let {
         return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setBankSlip(spotlightPrice.toBankSlip())
            .setCreditCards(scrapCreditCards(spotlightPrice))
            .build()
      }
      throw MalformedPricingException("SpotlightPrice", spotlightPrice)
   }

   private fun fetchPrices(internalId: String): JSONObject {
      val url = "https://redebella.com.br/index.php?route=extension/soconfig/liveprice/index"
      val headers: MutableMap<String, String> = HashMap()
      headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8"
      headers["Cookie"] = "OCSESSID=1989a1d35d74239395a9ab8e76; language=pt-br; currency=BRL"

      val payload = "product_id=$internalId"
      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build()

      return CrawlerUtils.stringToJson(FetcherDataFetcher().post(session, request).body)

   }

   private fun scrapCreditCards(spotlightPrice: Double): CreditCards {
      val creditCards = CreditCards()
      val installments = Installments()

      installments.add(
         InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build()
      )

      for (brand in cards) {
         creditCards.add(
            CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(installments)
               .build()
         )
      }
      return creditCards
   }
}
