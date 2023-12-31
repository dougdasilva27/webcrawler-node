package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.toBankSlip
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document

class SaopauloSupermercadotresmCrawler(session: Session) : Crawler(session) {

   private val SELLER_FULL_NAME: String = "Supermercado tresm"
   private val cards = listOf(
      Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString()
   )

   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)
      val products = mutableListOf<Product>()

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: ${session.originalURL}")

         val internalId = scrapInternalId(doc)
         val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productName", true)
         val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb.hidden-xs li a", true)
         val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".description"))
         val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image_zoom", listOf("src"), "https", "s3-sa-east-1.amazonaws.com")
         val availability = doc.selectFirst(".subinfo-container.notify-me-section") == null
         val offers = if (availability) scrapOffers(doc) else Offers()
         val ratingsReviews = scrapRating(doc)

         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setRatingReviews(ratingsReviews)
            .build()

         products.add(product)

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }
      return products
   }

   private fun scrapInternalId(doc: Document): String? {
      val idAvailable = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".checkout-container .add-item-to-cart #variant_id", "value")

      if (idAvailable == null) {
         return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".notify-me-form #stock_request_variant_id", "value")
      }

      return idAvailable
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".product-details") != null
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val pricing = scrapPricing(doc)

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

      val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal_price > span", null, false, ',', this.session)
      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal_price .rrf span", null, false, ',', this.session)

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(spotlightPrice.toBankSlip())
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .build()
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

   //When this crawler was made no product with rating was found
   private fun scrapRating(doc: Document): RatingsReviews {
      return RatingsReviews()
   }
}
