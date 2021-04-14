package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc

abstract class TieliSupermercadoRanking(session: Session) : CrawlerRankingKeywords(session) {

    protected abstract val emp: String

    private val searchUrl = "http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine2.jsp?emp=${emp}&a=0"

    override fun extractProductsFromCurrentPage() {
        val headers = mutableMapOf(
            "Content-Type" to "application/x-www-form-urlencoded",
            "Accept" to "*/*",
            "Cookie" to "JSESSIONID=5F52AB30D36C5E3446A46F1CDE121D33; __cfduid=d33f084f0b0c5802e097effe60c91b2101617986855"
        )
        val request = RequestBuilder.create()
            .setCookies(cookies)
            .setUrl(searchUrl)
            .setHeaders(headers)
            .setPayload("valor=$keywordEncoded&x=0&y=0").build()
        currentDoc = dataFetcher.post(session, request).body?.toDoc()

        currentDoc.select(".tbProduto").forEach { doc ->
            val internalId = doc.selectFirst(".tbProduto b").text().split("-").last()
            saveDataProduct(internalId, null, "http://compras.tieli.com.br:38999/$internalId")
        }
    }

    override fun checkIfHasNextPage(): Boolean {
        return false
    }
}
