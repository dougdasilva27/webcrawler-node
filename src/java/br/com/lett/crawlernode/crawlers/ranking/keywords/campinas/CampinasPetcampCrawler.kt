package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class CampinasPetcampCrawler(session: Session?) : CrawlerRankingKeywords(session) {

    override fun extractProductsFromCurrentPage() {
        pageSize = 12
        log("Página $currentPage")

        val url = "https://www.petcamp.com.br/buscapagina?ft=$keywordEncoded&PS=12&sl=5e1350fe-8755-42e7-acd1-7682c25399f5&cc=12&sm=0&PageNumber=$currentPage"
        log("Link onde são feitos os crawlers: $url")

        currentDoc = fetchDocument(url)

        val products = currentDoc.select(".sawi-shelf ul li .sawi-shelf-box")

        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts()
            }
            for (product in products) {
                val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".buy-button-normal", "id")
                val internalPid = internalId
                val productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".sawi-shelf-image a", "href")
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
      val url = "https://www.petcamp.com.br/$keywordEncoded"
      val page = fetchDocument(url)
      val json = CrawlerUtils.selectJsonFromHtml(page, "script", "vtex.events.addData(", ");", false, false)
      totalProducts = json.optInt("siteSearchResults")
      println(totalProducts)
      log("Total da busca: $totalProducts")
   }

}
