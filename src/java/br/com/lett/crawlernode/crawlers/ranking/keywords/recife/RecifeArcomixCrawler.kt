package br.com.lett.crawlernode.crawlers.ranking.keywords.recife

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import org.apache.http.HttpHeaders.CONTENT_TYPE
import org.json.JSONArray
import org.json.JSONObject

class RecifeArcomixCrawler(session: Session?) : CrawlerRankingKeywords(session) {

   init {
      super.fetchMode = FetchMode.FETCHER
   }

   private val idArmazem = session!!.options.optString("id_armazem")

   override fun extractProductsFromCurrentPage() {
        pageSize = 30
       val url = "https://arcomix.com.br/api/busca"
       val payload = """
          {
          "avaliacoes": [],
          "categorias": [],
          "descricao": "$keywordWithoutAccents",
          "marcas": [],
          "num_reg_pag": $pageSize,
          "order": "MV",
          "pg": $currentPage,
          "precoFim": 0,            
          "precoIni": 0,          
          "subcategorias": [],            
          "visualizacao": "CARD"
          }
          """.trimIndent()

       val headers: MutableMap<String, String> = HashMap()
       headers["Cookie"] = "ls.uid_armazem=$idArmazem"
       headers["Content-Type"] = "application/json;charset=UTF-8"

        val request = RequestBuilder.create().setUrl(url).setPayload(payload).setHeaders(headers)
            .build()

        val json = dataFetcher.post(session, request).body

        val productsJson = JSONUtils.stringToJson(json)?.optJSONArray("Produtos")?: JSONArray()

        for (productJson in productsJson) {
            if (productJson is JSONObject) {
                val internalId = productJson.optString("id_produto",null)
                val productUrl = "https://arcomix.com.br/produto/${productJson.optString("str_link_produto")}"
                saveDataProduct(internalId, null, productUrl)
                log("""Position: $position - InternalId: $internalId - Url: $productUrl""")
            }
        }

        log("""Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados""")
    }

    override fun hasNextPage(): Boolean {
        return (arrayProducts.size % pageSize - currentPage) < 0
    }
}
