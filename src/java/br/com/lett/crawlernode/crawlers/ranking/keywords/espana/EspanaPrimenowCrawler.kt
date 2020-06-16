package br.com.lett.crawlernode.crawlers.ranking.keywords.espana

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import org.jsoup.nodes.Element

class EspanaPrimenowCrawler(session: Session) : CrawlerRankingKeywords(session) {

  override fun extractProductsFromCurrentPage() {
    currentDoc = fetchDocument("https://primenow.amazon.es/search?k=$keywordEncoded&ref_=pn_gw_nav_sr_ALL&page=$currentPage")
    for (element: Element in currentDoc.select(".asin_card__root__3x1lV")) {
      val url = element.selectFirst("a")?.attr("href")
      val internalId = url?.substringBefore("?")?.substringAfter("dp/")
      saveDataProduct(internalId, null, "https://primenow.amazon.es$url")
      log("internalId - $internalId url - https://primenow.amazon.es$url")
    }
  }

  override fun checkIfHasNextPage(): Boolean {
    return currentDoc.select(".buttons__prev-next-button__h1qhS").size == 2
  }
}
