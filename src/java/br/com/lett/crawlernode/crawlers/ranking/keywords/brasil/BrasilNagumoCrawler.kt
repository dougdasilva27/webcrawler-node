package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BrasilNagumoCrawler(session: Session) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 20
        log("Página $currentPage")
        val url = "https://www.nagumo.com.br/buscapagina?ft=$keywordEncoded&PS=20&sl=c7c7de7b-0063-4ce3-bd0c-43571ae046f1&cc=20&sm=0&PageNumber=$currentPage"
        log("Link onde são feitos os crawlers: $url")
        currentDoc = fetchDocument(url)

        val products = currentDoc.select(".prateleira > ul > li > :not(helperComplement)")
        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a[rel]", "rel")
                val productUrl = CrawlerUtils.scrapUrl(product, "a[href^=\"https\"]", "href", "https://", "www.nagumo.com.br")
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
        val url = "https://www.nagumo.com.br/busca/?ft=$keywordEncoded"
        val document = fetchDocument(url)
        totalProducts = CrawlerUtils.scrapIntegerFromHtml(document, ".resultado-busca-numero .value", true, 0)
        log("Total da busca: $totalProducts")
    }
}