package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.Logging
import models.prices.Prices
import org.jsoup.nodes.Document
import java.math.RoundingMode

class SaopauloMarcheCrawler(session: Session?) : Crawler(session) {

    private val home = "https://www.marche.com.br/"

    override fun shouldVisit(): Boolean {
        val href = session.originalURL.toLowerCase()
        return !FILTERS.matcher(href).matches() && href.startsWith(home)
    }

    override fun extractInformation(document: Document?): MutableList<Product> {
        val products = mutableListOf<Product>()
        val json = JSONUtils.stringToJson(CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "div[data-json]",
                "data-json"))

        json ?: Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)

        json?.let {
            val price = it.optFloat("price")
            val prices = scrapPrices(document, price)

            val categories = mutableListOf<String>()

            if (it.optString("parent_category") is String) {
                categories.add(it.optString("parent_category"))
            }
            if (it.optString("category") is String) {
                categories.add(it.optString("category"))
            }

            val eans = mutableListOf<String>()
            if (it.optString("ean") is String) {
                categories.add(it.optString("ean"))
            }

            products.add(ProductBuilder.create()
                    .setUrl(session.originalURL)
                    .setInternalId(it.optString("product_id"))
                    .setInternalPid(it.optString("id"))
                    .setName(it.optString("full_name"))
                    .setPrice(price)
                    .setPrices(prices)
                    .setAvailable(document?.selectFirst(".btn.btn-block.btn-lg.center-y") != null)
                    .setCategories(categories)
                    .setPrimaryImage(it.optString("image"))
                    .setEans(eans)
                    .build())
        }
        return products
    }

    private fun scrapPrices(doc: Document?, price: Float) = Prices().apply {
        this.priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price> span", null, false, ',', session)
        this.bankTicketPrice = price.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val installments: MutableMap<Int, Float> = HashMap()
        installments[1] = price
        this.insertCardInstallment(Card.VISA.toString(), installments)
        this.insertCardInstallment(Card.MASTERCARD.toString(), installments)
        this.insertCardInstallment(Card.ELO.toString(), installments)
    }
}
