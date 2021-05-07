package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.nodes.Element

class RiodejaneiroSuperprixCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val home = "https://www.superprix.com.br/"

   val token: String by lazy {
      fetchDocument(home).selectFirst("li[layout]").attr("layout")
   }

   init {
      pageSize = 15
   }

   override fun extractProductsFromCurrentPage() {
      val url = home + "buscapagina?ft=${keywordWithoutAccents.replace(" ", "+")}" +
         "&PS=15&sl=$token&cc=15&sm=0&PageNumber=$currentPage"
      val fetchDocument = fetchDocument(url, cookies)
      fetchDocument.select(".prateleira li[layout]")?.forEach {
         val internal = it.selectFirst(".buy-button-normal")?.attr("id")
         val pid = it.selectFirst(".avaliacao div")?.attr("id")?.split("-")?.last()
         val productUrl = it.selectFirst("a")?.attr("href")
         log("InternalId: $internal - pid: $pid - Url: $productUrl")
         saveDataProduct(internal, pid, productUrl)
      }
   }

   override fun checkIfHasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }
}
