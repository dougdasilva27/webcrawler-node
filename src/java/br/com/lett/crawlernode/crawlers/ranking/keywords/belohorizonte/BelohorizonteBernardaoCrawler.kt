package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BelohorizonteBernardaoCrawler(session: Session) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 20
        log("Página $currentPage")

        val url = "https://www.bernardaoemcasa.com.br/catalogsearch/result/index/?p=$currentPage&q=$keywordEncoded"
        log("Link onde são feitos os crawlers: $url")

        currentDoc = fetchDocument(url)

        val products = currentDoc.select(".product-item")

        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {

                val internalPid = CrawlerUtils.scrapIntegerFromHtmlAttr(product, ".regular-price", "id", 0).toString()
                val productUrl: String = CrawlerUtils.scrapUrl(product, ".cdz-product-top > a", "href", "https://", "www.bernardaoemcasa.com.br")
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
        log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados")
    }

    override fun setTotalProducts() {
        totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".pager .amount","de","",true, true, 0)
        log("Total da busca: $totalProducts")
    }
}