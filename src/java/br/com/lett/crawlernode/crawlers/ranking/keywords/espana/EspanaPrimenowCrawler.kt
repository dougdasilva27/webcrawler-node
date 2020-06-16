package br.com.lett.crawlernode.crawlers.ranking.keywords.espana

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toInt
import org.jsoup.nodes.Element

class EspanaPrimenowCrawler(session: Session) : CrawlerRankingKeywords(session) {

  init {
    pageSize = 30
  }

  override fun extractProductsFromCurrentPage() {
    currentDoc = fetchDocument("https://primenow.amazon.es/search?k=$keywordEncoded&ref_=pn_gw_nav_sr_ALL&page=$currentPage")
    for (element: Element in currentDoc.select(".asin_card__root__3x1lV")) {
      val url = element.selectFirst("a")?.attr("href")
      val internalId = url?.substringBefore("?")?.substringAfter("dp/")
      saveDataProduct(internalId, null, "https://primenow.amazon.es$url")
      log("internalId - $internalId url - https://primenow.amazon.es$url")
    }
  }

  override fun setTotalProducts() {
    totalProducts = currentDoc.selectFirst(".index__root__3XLxs div")?.toInt() ?: 0
  }
}
