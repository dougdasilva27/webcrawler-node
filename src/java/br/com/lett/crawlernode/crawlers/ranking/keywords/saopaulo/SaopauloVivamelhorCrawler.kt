package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.int

class SaopauloVivamelhorCrawler(session: Session) : CrawlerRankingKeywords(session) {
  init {
    pageSize = 20
  }

  override fun extractProductsFromCurrentPage() {
    val url = "https://www.lojavivamelhoremporio.com.br/buscar?search=$keywordEncoded&page=$currentPage"

    currentDoc = fetchDocument(url)
    currentDoc.select(".span3.product-block")?.forEach {
      val internal = it.selectFirst("input[name='product_id']").attr("value")
      val productUrl = it.selectFirst(".image a.img").attr("href").substringBefore("?")
      log("internal $internal - url $productUrl")
      saveDataProduct(internal, null, productUrl)
    }

    if (totalProducts == 0) {
      setTotalProducts()
    }
  }

  override fun setTotalProducts() {
    totalProducts = currentDoc.selectFirst(".results strong:nth-child(2)").text().int() ?: 0
  }
}