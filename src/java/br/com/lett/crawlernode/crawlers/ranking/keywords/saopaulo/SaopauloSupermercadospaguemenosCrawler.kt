package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords

class SaopauloSupermercadospaguemenosCrawler(session: Session?) : CrawlerRankingKeywords(session) {

  init {
    pageSize = 40
  }

  override fun extractProductsFromCurrentPage() {
    val url = "https://www.superpaguemenos.com.br/$keywordEncoded/?p=$currentPage"

    currentDoc = fetchDocument(url)
    for (element in currentDoc.select(".item-product")) {
      val internalId = element.attr("data-id")?.replace("sku_", "")
      val productUrl = element.selectFirst("meta").attr("content")?.replaceFirst("http", "https")
      saveDataProduct(internalId, null, productUrl)
      log("internalId $internalId - url $productUrl")
    }
  }

  override fun hasNextPage() = currentDoc.selectFirst(".next") != null
}
