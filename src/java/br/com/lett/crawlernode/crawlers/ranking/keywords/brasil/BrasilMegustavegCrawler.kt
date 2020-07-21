package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BrasilMegustavegCrawler(session: Session) : CrawlerRankingKeywords(session) {

    private val BASE_URL: String = "www.megustaveg.com.br"

    override fun extractProductsFromCurrentPage() {
        pageSize = 24
        log("Página $currentPage")

        val url = "https://www.megustaveg.com.br/buscar?q=${this.keywordEncoded}&pagina=${this.currentPage}"
        log("Link onde são feitos os crawlers: $url")

        currentDoc = fetchDocument(url)

        val products = currentDoc.select("#listagemProdutos .row-fluid li")

        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".trustvox-stars", "data-trustvox-product-code")
                val internalPid = internalId
                val productUrl = CrawlerUtils.scrapUrl(product, ".info-produto > a", "href", "http", BASE_URL)
                saveDataProduct(null, null, productUrl)

                log("Position: $position - InternalId: $internalId - InternalPid: $internalPid - Url: $productUrl")
                if (arrayProducts.size == productsLimit) {
                    break
                }
            }
        } else {
            result = false
            log("Keyword sem resultado!")
        }
        log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados")
    }

    override fun hasNextPage(): Boolean {
        return this.currentDoc.selectFirst(".pagination li[class=\"disabled\"] > a[rel=\"next\"]") == null
    }
}