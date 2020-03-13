package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import org.jsoup.Jsoup

class SaopauloMarcheCrawler(session: Session?) : CrawlerRankingKeywords(session) {
    val home = "https://www.marche.com.br"
    override fun extractProductsFromCurrentPage() {
        val url = "https://www.marche.com.br/search?utf8=%E2%9C%93&query=$keywordEncoded&page=$currentPage"
        val request = RequestBuilder.create().setUrl(url)
                .setCookies(cookies).build()

        log("Link onde são feitos os crawlers: $url")

        Jsoup.parse(dataFetcher.get(session, request).body)?.let { doc ->
            val productsElements = doc.select("div[data-infinite-scroll='items'] > div[data-json]")
            this.pageSize = productsElements.size

            productsElements.asSequence()
                    .map { JSONUtils.stringToJson(it.attr("data-json")) }
                    .forEachIndexed { index, jsonProd ->
                        val urlProd = home + doc.select("a[class=link]")?.get(index)?.attr("href")
                        saveDataProduct(jsonProd.optString("product_id"), jsonProd.optString("id"), urlProd)
                        log("Position: $position - InternalPid: ${jsonProd.optString("id")} - Url: $urlProd")
                    }
        }
        log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
    }

    override fun hasNextPage(): Boolean {
        return this.pageSize >= 20
    }
}
