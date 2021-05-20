package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class BrasilSodimacCrawler(session: Session?) : CrawlerRankingKeywords(session) {
   
   init {
      pageSize = 28
   }

   override fun extractProductsFromCurrentPage() {
      val url = "https://www.sodimac.com.br/sodimac-br/search?Ntt=$keywordEncoded&currentpage=$currentPage"
      val doc = fetchDocument(url)

      val elements = doc?.select("div.search-results-products-container > div")!!

      for (elem in elements) {
         if (elem is Element) {
            val internal = elem.attr("data-key")!!
            val productUrl = "https://www.sodimac.com.br${elem.selectFirst("a.link-primary")?.attr("href")!!}"

            saveDataProduct(internal, null, productUrl)
            log("InternalId: $internal - Url: $productUrl")
         }
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
