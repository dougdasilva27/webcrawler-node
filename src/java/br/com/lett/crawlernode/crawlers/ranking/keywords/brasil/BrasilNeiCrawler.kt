package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.MathUtils
import org.jsoup.Jsoup

class BrasilNeiCrawler(session: Session?) : CrawlerRankingKeywords(session) {
    override fun extractProductsFromCurrentPage() {
        val url = "https://www.nei.com.br/produtos?utf8=%E2%9C%93&q=$keywordEncoded&page=$currentPage"
        val req = RequestBuilder.create().setUrl(url).setCookies(cookies).build()

        log("Link onde são feitos os crawlers: $url")

        currentDoc = Jsoup.parse(dataFetcher.get(session, req).body)

        val products = currentDoc.select(".products-container div[class='product']")
        pageSize = products.size
        products?.forEach { elem ->
            val internalId = elem?.attr("data-sku")
            val productUrl = elem?.selectFirst(".product-item__name > a")?.attr("href")
            saveDataProduct(internalId, null, productUrl)
            log("Position: $position - InternalId: $internalId - Url: $productUrl")
        }
    }

    override fun setTotalProducts() {
        totalProducts = MathUtils.parseInt(currentDoc.selectFirst(".total-products-search").text()) ?: pageLimit
        log("Total da busca: $totalProducts")
    }

    override fun hasNextPage(): Boolean {
        return currentDoc.select("a[rel='next']").size != 0
    }
}