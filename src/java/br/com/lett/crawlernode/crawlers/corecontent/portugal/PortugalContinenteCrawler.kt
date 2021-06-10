package br.com.lett.crawlernode.crawlers.corecontent.portugal

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toDoc
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document

class PortugalContinenteCrawler(session: Session) : Crawler(session) {

   init {
      super.config.fetcher = FetchMode.FETCHER
   }

   override fun fetch(): Document {

      val headers = mutableMapOf<String, String?>()

      // this will work for a short time, we need another solution!!
      headers.put(
         "cookie", "GCLB=CP_3z4bM6M2_8gE; rbzid=N9FmHzzU0zn3RmiaPjnOOXDuHpqeByEPuJd93/SiszJphdTgJ4euaLjGs2a4atQ/0J9xtoSGH90klfcO/FpciJ5/1" +
         "y4Q5IGnjuWrtyDCM6mcVeSvYGFbxxPYooSFt2RxxvG5VjaJgeBvlfqU+8HtyNrue+DFUey9sgM18iPmxoZhBC/Ezm7EGSRLvqM+NWhQdmHmR6CiNfwgqxbpPhLjTqZ8OLkorqK+Y9HID/D4ZtaR" +
         "CIjngexd0RstEiYIN13HTPyNYi9XjbBG+PNdyJMHNQ==; rbzsessionid=4673fa3d131c1edbc4908b0872b47060"
      )
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.135 Safari/537.36")

      val request = RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(session.originalURL)
         .setProxyservice(listOf(ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY))
         .build()
      val response = ApacheDataFetcher().get(session, request)

      return response.body.toDoc() ?: throw IllegalStateException("Could not retrieve html")
   }

   override fun extractInformation(document: Document): MutableList<Product> {
      val products = mutableListOf<Product>()

      if (document.selectFirst(".product-wrapper") != null) {

         val name = scrapName(document)
         val internalId = document.selectFirst(".add-to-cart")?.attr("data-pid")
         val description = document.selectFirst(".description-and-detail")?.text()

         val offers = scrapOffers(document)

         val primaryImg = document.selectFirst(".ct-product-image")?.attr("src")

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

   fun scrapName(doc: Document): String {
      val name = StringBuilder()
      name.append(doc.selectFirst(".product-name")?.text() ?: "")
      name.append(" ")
      name.append(doc.selectFirst(".ct-pdp--brand")?.text() ?: "")
      name.append(" ")
      name.append(doc.selectFirst(".ct-pdp--unit")?.text() ?: "")

      return name.toString().trim()
   }

   fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val price = doc.selectFirst(".ct-price-formatted")?.toDoubleComma()
      val priceFrom = doc.selectFirst(".ct-tile--price-dashed")?.toDoubleComma()

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
            .setSellerFullName("Continente Portugal")
            .build()
      )
      return offers
   }
}
