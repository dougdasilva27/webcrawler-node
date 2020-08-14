package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.eachText
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document

class SaopauloSupermercadospaguemenosCrawler(session: Session?) : Crawler(session) {

   override fun extractInformation(document: Document): MutableList<Product> {
      val products = mutableListOf<Product>()

      if ("/p" in session.originalURL) {

         val name = document.selectFirst("h1")?.text()
         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".bt-checkout", "data-sku")
         val categories = document.select("li a[itemprop='item'] span").eachText(ignoreIndexes = arrayOf(0))
         val offers = scrapOffers(document)
         val primaryImage = document.selectFirst(".cloud-zoom")?.attr("href")?.replaceFirst("//", "https://")

         // Creating the product
         products += ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .build()
      }

      return products
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val price = doc.selectFirst(".sale .sale_price")?.toDoubleComma()
      var priceFrom = doc.selectFirst(".box-pricing .price_off .unit_price")?.toDoubleComma()

      price?.let {
         if (price == priceFrom) {
            priceFrom = null
         }

         val creditCards = setOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX).toCreditCards(price)

         offers.add(
            OfferBuilder.create()
               .setPricing(
                  PricingBuilder.create()
                     .setCreditCards(creditCards)
                     .setSpotlightPrice(price)
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
}
