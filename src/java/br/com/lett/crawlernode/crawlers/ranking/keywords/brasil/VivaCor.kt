package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils

class VivaCor(session: Session?) : CrawlerRankingKeywords(session) {
   override fun extractProductsFromCurrentPage() {
      pageSize = 24
      val url = "https://www.vivacortintas.com/busca?search=$keywordEncoded&page=$currentPage"
      currentDoc = fetchDocument(url)
      val products = currentDoc.select(".product-layout.product-grid")
      for (e in products) {
         val internalPid = e.selectFirst("meta").attr("content")
         val productUrl = e.selectFirst("a").attr("href")
         saveDataProduct(null, internalPid, productUrl)
      }
   }

   override fun hasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
