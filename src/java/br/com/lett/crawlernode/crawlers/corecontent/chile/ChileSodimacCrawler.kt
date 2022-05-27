package br.com.lett.crawlernode.crawlers.corecontent.chile

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.SodimacCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toDoubleDot
import models.pricing.CreditCards
import models.pricing.Pricing
import org.jsoup.nodes.Document

class ChileSodimacCrawler(session: Session?): SodimacCrawler(session) {

   override fun scrapPricing(doc: Document): Pricing {
      val spotlightPrice = doc.selectFirst("div.main div.price").toDoubleDot()
      var priceFrom: Double? = null

      if (doc.selectFirst("div.sub") != null) {
         priceFrom = doc.selectFirst("div.sub div.price").toDoubleDot()
      }

      val bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null)
      val creditCards: CreditCards = scrapCreditCards(spotlightPrice)

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build()
   }
}
