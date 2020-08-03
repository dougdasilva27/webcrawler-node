package br.com.lett.crawlernode.crawlers.ranking.keywords.belgium

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class BelgiumCarrefourCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val BASE_URL = "drive.carrefour.eu"

   override fun extractProductsFromCurrentPage() {
      pageSize = 24
      log("Página $currentPage")
      val url = "https://drive.carrefour.eu/fr/search?q=${this.keywordEncoded}&page=${this.currentPage - 1}"
      log("Link onde são feitos os crawlers: $url")
      currentDoc = fetchDocument(url)

      val products = currentDoc.select(".product-item")
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         for (product in products) {
            val internalId = product.attr("data-productid")
            val internalPid = internalId
            val productUrl = CrawlerUtils.scrapUrl(product, ".thumb > a", "href", "https", BASE_URL)

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

      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados")
   }

   override fun setTotalProducts() {
      totalProducts = CrawlerUtils.scrapIntegerFromHtmlAttr(this.currentDoc, ".totalProducts", "value", 0)
      log("Total da busca: $totalProducts")
   }
}
