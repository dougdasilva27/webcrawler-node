package br.com.lett.crawlernode.crawlers.ranking.keywords.recife

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

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
            val name = productJson.optString("str_nom_produto",null)
            val imgUrl = productJson.optString("str_img_path",null)
            val price: Int = scrapPrice(productJson)

            val isAvailable = price != 0

            val productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build()

            saveDataProduct(productRanking)
         }
      }

      log("""Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} produtos crawleados""")
   }

   override fun hasNextPage(): Boolean {
      return (arrayProducts.size % pageSize - currentPage) < 0
   }

   fun scrapPrice(productJson: JSONObject): Int {
      val priceDouble = productJson.optDouble("mny_vlr_produto_por")

      val priceInCents = priceDouble * 100

      return priceInCents.toInt()
   }
}
