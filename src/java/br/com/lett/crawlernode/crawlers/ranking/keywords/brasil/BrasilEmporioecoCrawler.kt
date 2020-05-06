package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toDoc

class BrasilEmporioecoCrawler(session: Session) : CrawlerRankingKeywords(session) {

  init {
    pageSize = 12
  }

  override fun extractProductsFromCurrentPage() {
    val request = RequestBuilder.create().setUrl("https://lojaemporioeco.com.br/page/$currentPage/?s=$keywordEncoded&post_type=product")
      .build()
    currentDoc = dataFetcher.get(session, request).body?.toDoc()

    currentDoc.select(".product_item--thumbnail .wrap-addto > a").forEach {
      val internalPid = it.attr("data-product_sku")
      val url = it.attr("href")
      saveDataProduct(null, internalPid, url)
      log("Position: $position - InternalPid: $internalPid - Url: $url")
    }
  }

  override fun hasNextPage(): Boolean {
    return currentDoc.selectFirst(".next.page-numbers") != null
  }
}
