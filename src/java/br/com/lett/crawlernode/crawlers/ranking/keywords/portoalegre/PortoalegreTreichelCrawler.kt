package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

class PortoalegreTreichelCrawler(session: Session) : CrawlerRankingKeywords(session) {
  val url = "https://delivery.atacadotreichel.com.br/api/busca"

  init {
    pageSize = 30
  }

  override fun extractProductsFromCurrentPage() {
    val payload = """{"descricao":"$keywordWithoutAccents","order":"MV","pg":$currentPage,"marcas":[],
      |"categorias":[],"subcategorias":[],"precoIni":0,"precoFim":0,"avaliacoes":[],"num_reg_pag":$pageSize,
      |"visualizacao":"CARD"}""".trimMargin()
    val json = dataFetcher.post(session, Request.RequestBuilder.create().setUrl(url).setPayload(payload).build()).body?.toJson()!!

    for (elem in json.optJSONArray("Produtos")) {
      if (elem is JSONObject) {
        val productUrl = """https://delivery.atacadotreichel.com.br/${elem.optString("str_link_produto")}"""
        val id = elem.optString("id_produto")
        log("Url $productUrl - internalId - $id")
        saveDataProduct(id, null, productUrl)
      }
    }
  }
}
