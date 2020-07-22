package br.com.lett.crawlernode.crawlers.ranking.keywords.campinagrande

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.jsoup.nodes.Element

class CampinagrandeRedebellaCrawler(session: Session) : CrawlerRankingKeywords(session) {

    private val BASE_URL: String = "redebella.com.br"

    override fun extractProductsFromCurrentPage() {
        pageSize = 25
        log("Página $currentPage")

        val url = "https://redebella.com.br/buscar?search=$keywordEncoded&page=$currentPage"
        log("Link onde são feitos os crawlers: $url")

        currentDoc = fetchDocument(url)

        val products = currentDoc.select(".product-layout")

        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {

                val internalId = extractInternalId(product)
                val internalPid = internalId
                val productUrl: String = CrawlerUtils.scrapUrl(product, ".image > a", "href", "https:", BASE_URL)
                saveDataProduct(internalId, internalPid, productUrl)

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

    override fun setTotalProducts() {
        val fullText = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".row .text-left", true)
        val split = fullText.split("de", "(")
        totalProducts = split[split.lastIndex - 1].trim().toInt()
        log("Total da busca: $totalProducts")
    }

    private fun extractInternalId(product: Element): String {
        val fullText = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".list-block .addToCart", "onclick")
        return fullText.split("'")[1]
    }
}