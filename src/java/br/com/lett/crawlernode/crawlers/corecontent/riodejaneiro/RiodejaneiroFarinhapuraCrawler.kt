package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.addNonNull
import br.com.lett.crawlernode.util.eachText
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer
import models.Offers
import models.pricing.BankSlip
import models.pricing.Pricing
import org.jsoup.nodes.Document

class RiodejaneiroFarinhapuraCrawler(session: Session?) : Crawler(session) {

   private val sellerName = "Farinha Pura"

   override fun extractInformation(doc: Document): MutableList<Product> {
      val name = doc.select("font-weight-bold").text()
      val prodOption = doc.selectFirst(".flex-wrap.ml-0 .product-options")
      val categories = doc.select(".breadcrumb a")
         .eachText(ignoreIndexes = arrayOf(0), ignoreTokens = arrayOf(name, sellerName))
      val desc = doc.selectFirst(".information-body.p-0.py-3").wholeText()
      val images = mutableListOf<String>()
      images addNonNull doc.select(".sp-wrap a")?.attr("href")

      return mutableListOf(
         ProductBuilder.create()
            .setName(name)
            .setDescription(desc)
            .setPrimaryImage(images.removeFirstOrNull())
            .setSecondaryImages(images)
            .setOffers(scrapOffers(doc))
            .setInternalId(prodOption.selectFirst("input[name='sku']").`val`())
            .setInternalPid(prodOption.selectFirst("input[name='id']").`val`())
            .setCategories(categories)
            .build()
      )
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val price = doc.selectFirst(".h3.green")?.toDoubleComma()
      val priceFrom = doc.selectFirst(".price .woocommerce-Price-amount.amount")?.toDoubleComma()

      val creditCards = price?.let { listOf(Card.MASTERCARD, Card.VISA, Card.ELO, Card.AMEX, Card.HIPER, Card.HIPERCARD).toCreditCards(it) }

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .setPriceFrom(if ((priceFrom != 0.0) and (priceFrom != price)) priceFrom else null)
                  .setBankSlip(
                     BankSlip.BankSlipBuilder.create()
                        .setFinalPrice(price)
                        .build()
                  ).setCreditCards(creditCards)
                  .build()
            )
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(sellerName)
            .build()
      )

      return offers
   }
}
