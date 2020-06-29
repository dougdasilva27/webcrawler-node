package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BrasilLuccofitCrawler(session: Session) : CrawlerRankingKeywords(session) {

    private val HOME_PAGE = "https://www.luccofit.com.br/"

    override fun extractProductsFromCurrentPage() {
        pageSize = 12
        log("Página $currentPage")
        val url = "https://www.luccofit.com.br/busca?ft=$keywordEncoded&PageNumber=$currentPage"
        log("Link onde são feitos os crawlers: $url")
        currentDoc = fetchDocument(url)
        val products = currentDoc.select(".e-product")
        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".e-id", "value")
                val productUrl = CrawlerUtils.scrapUrl(product, ".e-product__lazyload > a", "href", "https://", HOME_PAGE)
                saveDataProduct(null, internalPid, productUrl)
                log("Position: $position - InternalId: null - InternalPid: $internalPid - Url: $productUrl")
                if (arrayProducts.size == productsLimit) break
            }
        } else {
            result = false
            log("Keyword sem resultado!")
        }
        log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
    }

    override fun setTotalProducts() {
        totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".resultado-busca-numero > .value", true, 0)
        log("Total da busca: $totalProducts")
    }
}