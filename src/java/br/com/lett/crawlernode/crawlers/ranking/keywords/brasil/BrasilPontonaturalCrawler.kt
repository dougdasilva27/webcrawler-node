package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toInt

class BrasilPontonaturalCrawler(session: Session) : CrawlerRankingKeywords(session) {

  override fun extractProductsFromCurrentPage() {
    currentDoc = fetchDocument(
      "https://www.pontonaturalshop.com.br/procurar?controller=search" +
          "&orderby=position" +
          "&orderway=desc" +
          "&search-cat-select=0" +
          "&search_query=$keywordEncoded" +
          "&p=$currentPage" +
          "&submit_search="
    )
    val elements = currentDoc.select(".product-container .right-block")
    for (elem in elements) {
      val internalId = elem.selectFirst(".product-reference").text()
      val productUrl = elem.selectFirst(".product-name").text()
      saveDataProduct(internalId, null, productUrl)
      log("internalId $internalId - $productUrl")
    }
  }

  override fun setTotalProducts() {
    totalProducts = currentDoc.selectFirst(".heading-counter").toInt()
  }
}
