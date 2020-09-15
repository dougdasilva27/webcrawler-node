package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.jsoup.nodes.Document

/**
 * Date: 15/09/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilMagodriveCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Magodrive"
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val name = doc.selectFirst(".information h3")?.ownText()

      val internalPid = doc.selectFirst(".wd-content input[name=ProductID]")?.attr("value")

      val internalId = doc.selectFirst(".wd-content input[name=SkuID]")?.attr("value")

      val description = doc.selectFirst(".description.full")?.html()

      val categories = CrawlerUtils.crawlCategories(doc, ".wd-browsing-breadcrumbs li:not(.first):not(.last)")

      val primaryImage = doc.selectFirst(".galeria .Image")?.attr("src")


      val offers = scrapOffers(doc)

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setDescription(description)
         .setOffers(offers)
         .setRatingReviews(null)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(doc: Document): Offers {

      val offers = Offers()

      val available = doc.selectFirst(".information .content[style=\"display:none\"] .bt-notifyme") != null

      if (!available) {
         return offers
      }

      val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".information .priceContainer .sale-price", null, false, ',', session)

      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".information .priceContainer .list-price span", null, false, ',', session)

      val bankSlip = spotlightPrice.toBankSlip()

      val creditCards = listOf(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.ELO, Card.DINERS).toCreditCards(spotlightPrice)

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(spotlightPrice)
                  .setPriceFrom(priceFrom)
                  .setCreditCards(creditCards)
                  .setBankSlip(bankSlip)
                  .build()
            )
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .build()
      )

      return offers


   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.select(".medias .information").isNotEmpty()
   }
}
