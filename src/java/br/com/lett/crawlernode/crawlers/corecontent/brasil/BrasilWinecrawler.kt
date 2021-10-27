package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import com.google.common.collect.Sets
import models.Offer
import models.Offers
import models.pricing.Installment
import models.pricing.Installments
import models.pricing.Pricing
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilWinecrawler(session: Session?) : Crawler(session) {

   companion object {
      private const val SELLER_FULL_NAME = "Wine"
   }

   protected var cards: Set<Card> = Sets.newHashSet(Card.VISA, Card.MASTERCARD,
      Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS)

   @Throws(Exception::class)
   override fun extractInformation(doc: Document): List<Product> {

      val products = mutableListOf<Product>()

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session,
            "Product page identified: " + session.originalURL)

         val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".PageHeader-title", false)

         val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a",false)

         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[sku-code]", "sku-code")

         val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".js-product-img", "src")

         val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".ReadMore-text",".TechnicalDetails"))

         val jsonOffer = CrawlerUtils.stringToJson(doc.selectFirst("price-box[campaigns]")?.attr(":product"))

         val isAvailable = jsonOffer.optBoolean("available",false)

         val offers = if(isAvailable) scrapOffers(jsonOffer) else Offers()

         val product = ProductBuilder()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
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

      val price : Double = jsonOffer.optDouble("salePrice")

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

}
