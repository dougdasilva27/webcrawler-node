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
import org.jsoup.nodes.Document
import java.util.*

class BrasilAbaraujoCrawler(session: Session?) : Crawler(session) {

   companion object {
      private const val SELLER_FULL_NAME = "Ab Araujo"
   }

   protected var cards: Set<Card> = Sets.newHashSet(Card.VISA, Card.MASTERCARD,
      Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS)

   @Throws(Exception::class)
   override fun extractInformation(doc: Document): List<Product> {

      val products = mutableListOf<Product>()

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session,
            "Product page identified: " + session.originalURL)

         val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail .product-name", false)

         val categories = CrawlerUtils.crawlCategories(doc, ".product-detail .breadcrumb .breadcrumb-item:not(:first-child):not(:last-child) a")

         val internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-reference", true)

         val images = scrapImages(doc)
         val primaryImage = if (images.isNotEmpty()) images.removeAt(0) else ""

         val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".product-tabs .description"))

         val isAvailable = doc.selectFirst(".produto-preco .PrecoPrincipal span") != null

         val offers = if (isAvailable) scrapOffers(doc) else Offers()

         val ean = scrapEan(doc);

         val product = ProductBuilder()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .setEans(Collections.singletonList(ean))
            .build()

         products.add(product)
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }
      return products
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".product-detail") != null
   }

   private fun scrapImages(doc: Document): MutableList<String> {
      val imgList: MutableList<String> = ArrayList()

      val elements = doc.select("div.image-show div.zoom img")
      for (el in elements) {
         imgList.add(el?.attr("data-src").toString())
      }
      return imgList
   }

   private fun scrapEan(doc: Document): String {
      var ean = ""
      val elements = doc.select("div#ficha tbody tr")

      for (el in elements) {
         if (el.toString().contains("código de barras")) {
            ean = el.select("td")?.last()?.html().toString()
            break
         }
      }
      return ean
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(scrapPricing(doc))
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

   private fun scrapPricing(doc: Document): Pricing {

      val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produto-preco .PrecoPrincipal span", null, false, ',', session)

      val bankSlip = price.toBankSlip()

      val installments = Installments()

      installments.add(
         Installment.InstallmentBuilder()
            .setInstallmentNumber(1)
            .setInstallmentPrice(price)
            .build()
      )

      val iNumber = CrawlerUtils.scrapIntegerFromHtml(doc, ".produto-preco .txt-corparcelas .preco-parc2 ", false, 1)

      val iPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produto-preco .txt-cadaparcelas .preco-parc2", null, false, ',', session)

      if (iNumber > 1) {
         installments.add(
            Installment.InstallmentBuilder()
               .setInstallmentNumber(iNumber)
               .setInstallmentPrice(iPrice)
               .build()
         )
      }

      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS).toCreditCards(installments)

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build()
   }

}
