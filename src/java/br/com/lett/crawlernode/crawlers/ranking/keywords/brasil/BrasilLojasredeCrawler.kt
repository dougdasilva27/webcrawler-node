package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords

class BrasilLojasredeCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 24
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.lojasrede.com.br/${keywordWithoutAccents.replace(" ", "%20")}?PageNumber=$currentPage"
      log("Link onde são feitos os crawlers: $url")
      currentDoc = fetchDocument(url, cookies)
      val products = currentDoc.select(".vitrine.resultItemsWrapper li[layout] span[data-id]")
         .distinctBy { element -> element.attr("data-id") }
      if (products.isNotEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         products.forEach { e ->
            val internalPid = e.attr("data-id")
            val productUrl = e.selectFirst(".product-name a")?.attr("href")
            saveDataProduct(null, internalPid, productUrl)

            log("Position: $position - InternalPid: $internalPid - Url: $productUrl")
         }
      }
      log("Pag $currentPage | ${arrayProducts.size} products")
   }

   override fun setTotalProducts() {
      val totalElement = currentDoc.selectFirst(".resultado-busca-numero .value")
      if (totalElement != null) {
         val text = totalElement.text().replace("[^0-9]".toRegex(), "").trim()
         if (text.isNotEmpty()) {
            totalProducts = text.toInt()
         }
         log("Total da busca: $totalProducts")
      }
   }
}
