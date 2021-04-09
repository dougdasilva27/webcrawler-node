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
import models.Offer
import models.Offers
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class TieliSupermercadoCrawler(session: Session) : Crawler(session) {

    protected abstract val emp: String
    private val internalId = session.originalURL.split("/").last();
    private val searchUrl = "http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine2.jsp?emp=$emp"

    private val sellerName = "Tieli Supermercado"

    override fun fetch(): Any {
        val request = RequestBuilder.create().setUrl(searchUrl).setPayload("valor=$internalId&x=0&y=0").build()
        return dataFetcher.post(session, request).body?.toDoc() ?: HttpGenericException("Fail retrieve body")
    }

    override fun extractInformation(doc: Document): MutableList<Product> {
        val productDoc = doc.select(".tbProduto").first { it.selectFirst("b").text().contains(internalId) }
        val name = productDoc.selectFirst("b").text()
        val offers = scrapOffers(productDoc)

        return mutableListOf(
            ProductBuilder.create()
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
            Offer.OfferBuilder.create()
                .setPricing(pricing)
                .setUseSlugNameAsInternalSellerId(true)
                .setSellerFullName(sellerName)
                .setIsMainRetailer(true)
                .build()
        )
        return offers
    }
}
