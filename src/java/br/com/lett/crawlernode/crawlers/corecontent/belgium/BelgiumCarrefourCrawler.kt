package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing
import models.pricing.Pricing.PricingBuilder
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.nodes.Document

abstract class BelgiumCarrefourCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.JSOUP
   }

   private val BASE_URL = "drive.carrefour.eu"
   private val SELLER_FULL_NAME = "carrefour"

   abstract fun getShopId(): String
   abstract fun getChosenDelivery(): String

   override fun handleCookiesBeforeFetch() {

      cookies.add(BasicClientCookie("Carrefour", "shopId:${getShopId()}"))
      cookies.add(BasicClientCookie("chosen-delivery", getChosenDelivery()))
      cookies.add(BasicClientCookie("chosen-delivery-address", getChosenDelivery()))
   }

   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)
      val products = mutableListOf<Product>()

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: ${session.originalURL}")
         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".pdp-titleSection", "data-productid")
         val internalPid = internalId
         val name = scrapNameAndBrand(doc)
         val categories = CrawlerUtils.crawlCategories(doc, "#cfbreadcrumbsdesktop .nounderline", true)
         val description = CrawlerUtils.scrapElementsDescription(doc, listOf(".description-section-inner > div[id]"))
         val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-gallery1 > div > img", listOf("src"), "https", BASE_URL)
         val secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#image-gallery1 > div > img", listOf("src"), "https", BASE_URL, primaryImage)
         val availability = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#slotUnAvailable", "value") == "false"
         val offers = if (availability) scrapOffers(doc) else Offers()

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
            .build()
         products.add(product)

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }

      return products
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst("#product-detail-container") != null
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val pricing: Pricing = scrapPricing(doc)

      offers.add(
         OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build()
      )
      return offers
   }

   private fun scrapPricing(doc: Document): Pricing {
      val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prod-price span", null, true, ',', this.session)
      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prod-priceCut", null, true, ',', this.session)

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(spotlightPrice.toBankSlip())
         .setCreditCards(listOf(Card.VISA, Card.MAESTRO, Card.MASTERCARD, Card.AMEX).toCreditCards(spotlightPrice))
         .build()
   }

   private fun scrapNameAndBrand(doc: Document): String? {

      var brandWithName = ""

      val brandName = CrawlerUtils.scrapStringSimpleInfo(doc, ".prod-brand", false)
      val justName = CrawlerUtils.scrapStringSimpleInfo(doc, ".prod-title", true)

      if (brandName != null) {
         val sb = StringBuilder()
         sb.append(brandName)
         sb.append(" ")
         sb.append(justName)
         brandWithName = sb.toString()

         return brandWithName
      }
      return justName
   }

}
