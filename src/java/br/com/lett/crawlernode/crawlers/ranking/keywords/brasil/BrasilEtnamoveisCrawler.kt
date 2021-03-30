package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toInt
import org.jsoup.select.Elements

class BrasilEtnamoveisCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 12
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.etna.com.br/search?pageSize=$pageSize&text=$keywordEncoded&page=${currentPage - 1}"
      currentDoc = fetchDocument(url)
      val products: Elements = currentDoc.select(".product__item--spot .thumb-img.js-gtm-clickProduct")
      if (products.isNotEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts()
         }
         products.forEach { e ->
            val internalId = e.attr("id")
            val urlProduct = "https://www.etna.com.br${e.attr("href")}"
            saveDataProduct(internalId, null, urlProduct)
            log("Position: $position - InternalId: $internalId - Url: $urlProduct")
         }
      }
   }

   override fun setTotalProducts() {
      totalProducts = currentDoc.selectFirst(".row .pagination-bar-results").toInt() ?: 0
   }
}
