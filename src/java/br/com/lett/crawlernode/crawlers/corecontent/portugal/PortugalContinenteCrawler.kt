package br.com.lett.crawlernode.crawlers.corecontent.portugal

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document

class PortugalContinenteCrawler(session: Session) : Crawler(session) {

  override fun extractInformation(document: Document): MutableList<Product> {
    val products = mutableListOf<Product>()
    if ("ProductId" in session.originalURL) {
      val name = document.selectFirst(".productTitle")?.text()
      val internalId = document.selectFirst(".ProductCode")?.attr("value")
      val description = document.selectFirst(".productDetailArea .productDetailSubArea")?.html()

      val offers = scrapOffers(document)

      val primaryImg = document.selectFirst("#zoomWindow div img")?.attr("src")


      products += ProductBuilder.create()
        .setUrl(session.originalURL)
        .setInternalId(internalId)
        .setName(name)
        .setPrimaryImage(primaryImg)
        .setDescription(description)
        .setOffers(offers)
        .build()

    }
    return products
  }

  fun scrapOffers(doc: Document): Offers {
    val offers = Offers()

    val price = doc.selectFirst(".updListPrice")?.toDoubleComma()
    val priceFrom = doc.selectFirst(".priceWas .pricePerUnit")?.toDoubleComma()

    val bankSlip = price?.toBankSlip()
    val pricing = PricingBuilder.create()
      .setSpotlightPrice(price)
      .setPriceFrom(priceFrom)
      .setBankSlip(bankSlip)
      .build()

    offers.add(
      OfferBuilder.create()
        .setIsBuybox(false)
        .setIsMainRetailer(true)
        .setPricing(pricing)
        .setUseSlugNameAsInternalSellerId(true)
        .setSellerFullName("Continente")
        .build()
    )
    return offers
  }
}
