package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords

class BrasilFastnutriCrawler(session: Session) : CrawlerRankingKeywords(session) {
  val token: String by lazy {
    fetchDocument("https://www.fastnutri.com.br/").selectFirst("li[layout]").attr("layout")
  }

  init {
    pageSize = 12
  }

  override fun extractProductsFromCurrentPage() {
    val url = "https://www.fastnutri.com.br/buscapagina?ft=kit&PS=12&sl=$token&cc=12&sm=0&PageNumber=$currentPage"

    currentDoc = fetchDocument(url)

    currentDoc.select("div[data-sku]")?.forEach {
      val internal = it.attr("data-sku")
      val internalPid = it.attr("data-id")
      val productUrl = it.selectFirst("a").attr("href")
      log("InternalId: $internal - InternalPid: $internalPid - Url: $productUrl")
      saveDataProduct(internal, internalPid, productUrl)
    }
  }

  override fun checkIfHasNextPage(): Boolean {
    return (arrayProducts.size % pageSize - currentPage) < 0
  }
}
