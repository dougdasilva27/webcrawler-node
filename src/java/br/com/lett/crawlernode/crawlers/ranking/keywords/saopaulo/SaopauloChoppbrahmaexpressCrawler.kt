package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toDoc

class SaopauloChoppbrahmaexpressCrawler(session: Session) : CrawlerRankingKeywords(session) {

    init {
        pageSize = 30
    }

    companion object {
        private const val HOME_PAGE = "https://www.choppbrahmaexpress.com.br/"
    }

    override fun extractProductsFromCurrentPage() {
        val url = "$HOME_PAGE$keywordEncoded?_q=$keywordEncoded&map=ft&page=$currentPage"
        val headers: MutableMap<String, String> = HashMap()
        headers["Content-Type"] = "text/html; charset=utf-8"
        val request =
            RequestBuilder.create().setUrl(url.replace("+", "%20"))
                .setHeaders(headers)
                .build()

        val strJson = dataFetcher.get(session, request).body?.toDoc()?.select("script")
            ?.first { element -> element.html().contains("__STATE__") }
            .toString().substringAfter("__STATE__ =").substringBeforeLast("}") + "}"

        val respMap = CrawlerUtils.stringToJSONObject(strJson).toMap()

        val products = respMap.filterKeys { key -> key.matches("Product:sp-[0-9]+\$".toRegex()) }
        pageSize = products.size
        if (products.isNotEmpty()) {
            if (this.totalProducts == 0) {
                val totalProductsMap = respMap
                    .filterValues { value -> (value is Map<*, *>) && value.containsKey("recordsFiltered") }.values.first()
                if (totalProductsMap is Map<*, *>) {
                    totalProducts = totalProductsMap["recordsFiltered"] as Int? ?: 0
                }
            }
            for (obj in products.values) {
                if (obj is Map<*, *>) {
                    val productUrl = "https://choppbrahmaexpress.com.br${obj["link"]}"
                    val internalPid = obj["productId"] as String
                    saveDataProduct(null, internalPid, productUrl)
                    log("Position: $position - InternalPid: $internalPid - Url: $productUrl")
                }
            }
        }
    }

    override fun checkIfHasNextPage(): Boolean {
        return (arrayProducts.size % pageSize - currentPage) < 0
    }
}
