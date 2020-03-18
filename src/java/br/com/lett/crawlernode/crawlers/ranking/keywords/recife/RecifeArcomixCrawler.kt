package br.com.lett.crawlernode.crawlers.ranking.keywords.recife

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import org.apache.http.HttpHeaders.CONTENT_TYPE
import org.apache.http.HttpHeaders.USER_AGENT
import org.json.JSONObject

class RecifeArcomixCrawler(session: Session?) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 30
        val url = "https://arcomix.com.br/api/categoria"
        val payload = """
            {
            "order": "MV",
            "pg": $currentPage,
            "marcas": [],
            "precoIni": 0,
            "precoFim": 0,
            "avaliacoes": [],
            "produto": "$keywordWithoutAccents",
            "num_reg_pag": $pageSize,
            "visualizacao": "CARD"
            }
            """.trimIndent()
        val headers = mutableMapOf(
            CONTENT_TYPE to "application/json; charset=utf-8",
            USER_AGENT to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/80.0.3987.132 Safari/537.36"
        )
        val request = RequestBuilder.create().setUrl(url).setPayload(payload).setHeaders(headers)
            .mustSendContentEncoding(false).build()
        val productsJson = JSONUtils.stringToJson(dataFetcher.post(session, request).body).optJSONArray("Produtos")

        for (productJson in productsJson) {
            if (productJson is JSONObject) {
                val internalId = productJson.optString("id_produto")
                val productUrl = "https://arcomix.com.br/produto/${productJson.optString("str_link_produto")}"
                saveDataProduct(internalId, null, productUrl)
                log("""Position: $position - InternalId: $internalId - Url: $productUrl""")
            }
        }

        log("""Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados""")
    }

    override fun hasNextPage(): Boolean {
        return (arrayProducts.size % pageSize - currentPage) < 0
    }
}
