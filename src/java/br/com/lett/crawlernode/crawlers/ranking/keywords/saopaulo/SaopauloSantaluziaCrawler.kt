package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class SaopauloSantaluziaCrawler(session: Session) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 48
        log("Página $currentPage")
        val url = "https://www.santaluzia.com.br/buscapagina?ft=$keywordEncoded&PS=48&sl=3c4f0b1f-8c42-410b-a078-96911a73069f&cc=48&sm=0&PageNumber=$currentPage"
        log("Link onde são feitos os crawlers: $url")
        currentDoc = fetchDocument(url)
        val products = currentDoc.select(".prateleira > ul > li > :not(helperComplement)")
        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "article[data-id]", "data-id")
                val productUrl = CrawlerUtils.scrapUrl(product, "a", "href", "https://", "www.santaluzia.com.br")
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
        val url = "https://www.santaluzia.com.br/$keywordEncoded?&utmi_p=_---200-1039849_p&utmi_pc=BuscaFullText&utmi_cp=$keywordEncoded"
        val document = fetchDocument(url)
        totalProducts = CrawlerUtils.scrapIntegerFromHtml(document, ".resultado-busca-numero .value", true, 0)
        log("Total da busca: $totalProducts")
    }

}