package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords

class BrasilLefarmaCrawler(session: Session?) : CrawlerRankingKeywords(session) {

  init {
    pageSize = 40
  }

  override fun extractProductsFromCurrentPage() {
    val url = "https://www.lefarma.com.br/buscar?q=$keywordEncoded&pagina=$currentPage"

    currentDoc = fetchDocument(url)

    val elements = currentDoc.select("#listagemProdutos .span3 > div")

    for (e in elements) {
      val internalId = e.selectFirst("div[data-trustvox-product-code]")?.attr("data-trustvox-product-code")
      val productUrl = e.selectFirst(".nome-produto")?.attr("href")
      saveDataProduct(internalId, null, productUrl)
      log("InternalId: null - Url: $productUrl")
    }
  }

  override fun setTotalProducts() {
    totalProducts = currentDoc.select("#listagemProdutos .span3 > div")?.size ?: 0
  }
}