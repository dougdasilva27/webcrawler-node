package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toInt

class BrasilPontonaturalCrawler(session: Session) : CrawlerRankingKeywords(session) {

  init {
    pageSize = 24
  }

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
    if (arrayProducts.size == 0) {
      setTotalProducts()
    }
    val elements = currentDoc.select(".product-container .right-block")
    for (elem in elements) {
      val internalId = elem.selectFirst(".product-reference").text()
      val productUrl = elem.selectFirst(".product-name").attr("href").substringBeforeLast("?")
      saveDataProduct(internalId, null, productUrl)
      log("internalId $internalId - $productUrl")
    }
  }

  override fun setTotalProducts() {
    totalProducts = currentDoc.selectFirst(".heading-counter").toInt() ?: 0
  }
}
