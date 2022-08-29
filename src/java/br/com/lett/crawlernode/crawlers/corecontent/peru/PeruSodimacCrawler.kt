package br.com.lett.crawlernode.crawlers.corecontent.peru

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.SodimacCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import models.pricing.CreditCards
import models.pricing.Pricing
import org.jsoup.nodes.Document

class PeruSodimacCrawler(session: Session?): SodimacCrawler(session) {

   override fun scrapPricing(doc: Document): Pricing {
      var spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".primary", null, false, '.', session)
      var priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".secondary", null, false, '.', session)

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
