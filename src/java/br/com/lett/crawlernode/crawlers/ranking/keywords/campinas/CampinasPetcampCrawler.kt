package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.jsoup.nodes.Element

class CampinasPetcampCrawler(session: Session?) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 24
        log("Página $currentPage")

        val url = "https://www.petcamp.com.br//resultadopesquisa?pag=$currentPage&departamento=&buscarpor=$keywordEncoded"
        log("Link onde são feitos os crawlers: $url")

        currentDoc = fetchDocument(url)

        val products = currentDoc.select(".produto")

        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".galeria_produto_comparar > input", "value")
                val internalPid = internalId
                val productUrl = scrapProductUrl(product)
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

    override fun setTotalProducts() {
        totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "#totalProducts", true, 0)
        log("Total da busca: $totalProducts")
    }

    private fun scrapProductUrl(product: Element): String? {
        val attrText = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".foto  > a", "href")
        val splittedText = attrText.split("'")
        return splittedText[splittedText.lastIndex - 1]
    }
}