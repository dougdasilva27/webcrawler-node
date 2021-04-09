package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc

abstract class TieliSupermercadoRanking(session: Session) : CrawlerRankingKeywords(session) {

    protected abstract val emp: String

    private val searchUrl = "http://compras.tieli.com.br:38999/KiProcesso/CompraOnLine2.jsp?emp=$emp"

    override fun extractProductsFromCurrentPage() {
        val request = RequestBuilder.create().setUrl(searchUrl).setPayload("valor=$keywordEncoded&x=0&y=0").build()
        currentDoc = dataFetcher.post(session, request).body?.toDoc()

        currentDoc.select(".tbProduto").forEach { doc ->
            val internalId = doc.selectFirst(".tbProduto b").text().split("-").last()
            saveDataProduct(internalId, null, "http://compras.tieli.com.br:38999/$internalId")
        }
    }
}
