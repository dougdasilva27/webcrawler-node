package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import org.jsoup.nodes.Element


class BrasilSodimacCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   private var isCategory = false
   private var urlCategory: String? = null

   init {
      pageSize = 28
   }

   override fun extractProductsFromCurrentPage() {
      var url = "https://www.sodimac.com.br/sodimac-br/search?Ntt=$keywordEncoded&currentpage=$currentPage"

      if (currentPage > 1 && isCategory) {
         url = "${urlCategory}&currentpage=$currentPage"
      }

      this.currentDoc = fetchDocument(url)

      if (this.currentPage == 1) {
         val redirectUrl = this.session.getRedirectedToURL(url)

         if (redirectUrl != null && !redirectUrl.equals(url)) {
            isCategory = true
            this.urlCategory = redirectUrl
         } else {
            isCategory = false
         }
      }

      val elements = this.currentDoc?.select("div.search-results-products-container > div")!!

      for (elem in elements) {
         if (elem is Element) {
            val internal = elem.attr("data-key")!!
            val productUrl = "https://www.sodimac.com.br${elem.selectFirst("a.link-primary")?.attr("href")!!}"

            saveDataProduct(internal, null, productUrl)
            log("${this.position} - InternalId: $internal - Url: $productUrl")
         }
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return this.currentDoc.selectFirst("button[id=bottom-pagination-next-page]") != null
   }
}
