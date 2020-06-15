package br.com.lett.crawlernode.crawlers.corecontent.espana

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.eachAttr
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing
import org.json.JSONArray
import org.jsoup.nodes.Document

class EspanaPrimenowCrawler(session: Session) : Crawler(session) {
  override fun extractInformation(document: Document): MutableList<Product> {
    val products = mutableListOf<Product>()
    val name = document.selectFirst("#productTitle")?.text()?.trim()
    val description = document.selectFirst("#productDescription_feature_div")?.html()
    val primaryImg = document.selectFirst("#landingImage")?.attr("src")
    val secondaryImgs = JSONArray()
    document.select(".a-spacing-small.item img")?.eachAttr("src", arrayOf(0))
      ?.forEach { img -> secondaryImgs.put(img.replaceFirst("38,50", "640,480")) }
    val internalId = session.originalURL.substringAfter("dp/").substringBefore("?")

    val offers = scrapOffers(document)

    products += ProductBuilder.create()
      .setUrl(session.originalURL)
      .setInternalId(internalId)
      .setName(name)
      .setPrimaryImage(primaryImg)
      .setSecondaryImages(secondaryImgs.toString())
      .setDescription(description)
      .setOffers(offers)
      .build();
    return products
  }

  private fun scrapOffers(document: Document): Offers {
    val offers = Offers()

    val price = document.selectFirst("#priceblock_ourprice")?.toDoubleComma()
    val priceFrom = document.selectFirst(".priceBlockStrikePriceString")?.toDoubleComma()


    val pricing = Pricing.PricingBuilder.create()
      .setPriceFrom(priceFrom)
      .setSpotlightPrice(price)
      .setBankSlip(price?.toBankSlip()).build()
    val offer = OfferBuilder.create()
      .setUseSlugNameAsInternalSellerId(true)
      .setPricing(pricing)
      .setSellerFullName(document.selectFirst("#merchant-info")?.text()?.trim()?.substringAfter("por "))
      .setIsBuybox(false)
      .build()

    offers.add(offer)

    return offers
  }
}