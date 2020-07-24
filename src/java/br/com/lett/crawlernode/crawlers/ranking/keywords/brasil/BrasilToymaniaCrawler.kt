package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BrasilToymaniaCrawler(session: Session) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 16
        log("Página $currentPage")
        val keyword = this.keywordWithoutAccents.replace(" ", "%20");
        val url = "https://www.toymania.com.br/$keyword?PS=16&PageNumber=$currentPage"
        log("Link onde são feitos os crawlers: $url")
        currentDoc = fetchDocument(url)

        val products = currentDoc.select(".x-shelf__item")
        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalPid = product.attr("data-product-id")
                val productUrl = CrawlerUtils.scrapUrl(product, "a.x-shelf__buy-btn[href]", "href", "https", "www.toymania.com.br")
                
                saveDataProduct(null, internalPid, productUrl)
                log("Position: $position - InternalId: null - InternalPid: $internalPid - Url: $productUrl")
                
                if (arrayProducts.size == productsLimit) {
                    break
                }
            }
        } else {
            result = false
            log("Keyword sem resultado!")
        }
		
        log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
    }

    override fun setTotalProducts() {
        totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero .value", true, 0)
        log("Total da busca: $totalProducts")
    }
}