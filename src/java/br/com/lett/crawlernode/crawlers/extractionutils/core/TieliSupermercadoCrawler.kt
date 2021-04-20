package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.exceptions.HttpGenericException
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toDoc
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class TieliSupermercadoCrawler(session: Session) : Crawler(session) {

    protected abstract val emp: String
    private val internalId = session.originalURL.split("/").last();
    private val searchUrl = "http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine2.jsp?emp=${emp}&a=0"

    private val sellerName = "Tieli Supermercado"

   override fun handleCookiesBeforeFetch() {
      cookies.addAll(dataFetcher.get(session, RequestBuilder.create().setUrl("http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine1.jsp?dp=PC").build()).cookies)
   }
    override fun fetch(): Any {
        val headers = mutableMapOf(
            "Cookie" to "JSESSIONID=${cookies.first { it.name == "JSESSIONID" }.value}; __cfduid=d33f084f0b0c5802e097effe60c91b2101617986855"
        )
        val request =
            RequestBuilder.create().setHeaders(headers).setUrl(searchUrl).setPayload("valor=$internalId&x=0&y=0")
                .build()
        return dataFetcher.post(session, request).body?.toDoc() ?: HttpGenericException("Fail retrieve body")
    }

    override fun extractInformation(doc: Document): MutableList<Product> {
        val productDoc = doc.select(".tbProduto").first { it.select("b").text().contains(internalId) }
        val name = productDoc.selectFirst("b").text()
        val offers = scrapOffers(productDoc)

        return mutableListOf(
            ProductBuilder.create()
                .setInternalId(internalId)
                .setUrl(session.originalURL)
                .setOffers(offers)
                .setName(name)
                .build()
        )
    }

    fun scrapOffers(productDoc: Element): Offers {
        val offers = Offers()

        val price = productDoc.selectFirst("b b").toDoubleComma()
        val pricing = PricingBuilder.create().setBankSlip(price?.toBankSlip())
            .setSpotlightPrice(price)
            .build()

        offers.add(
            OfferBuilder.create()
                .setPricing(pricing)
                .setUseSlugNameAsInternalSellerId(true)
                .setSellerFullName(sellerName)
                .setIsMainRetailer(true)
                .setIsBuybox(false)
                .build()
        )
        return offers
    }
}
