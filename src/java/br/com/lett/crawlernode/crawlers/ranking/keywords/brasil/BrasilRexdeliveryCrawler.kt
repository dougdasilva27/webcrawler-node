package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

/**
 * Date: 02/09/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilRexdeliveryCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private fun fetchApiToken(): String {
      val url = "https://api.rexdelivery.com.br/v1/auth/loja/login"

      val payload = """
          {
             "domain":"rexdelivery.com.br",
             "username":"loja",
             "key":"df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469"
          }
          """.trimIndent()

      val headers = mapOf(
         "content-type" to "application/json",
         "origin" to "https://www.rexdelivery.com.br"
      )

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build()

      val response = this.dataFetcher.post(session, request)

      if (response.lastStatusCode == 200) {
         val body = response.body.toJson()

         if (body.has("success") && body.optBoolean("success")) {
            return "Bearer ${body.getString("data")}"
         }
      }

      return ""
   }


   private fun fetchData(apiToken: String, currentPage: Int): JSONObject {
      val url = "https://api.rexdelivery.com.br/v1/loja/buscas/produtos/filial/1/centro_distribuicao/1/termo/" +
         keywordEncoded +
         "?page=${currentPage}"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(mapOf("authorization" to apiToken))
         .build()

      val response = dataFetcher[session, request]

      val body = response.body.toJson()

      if (body.has("success") && body.optBoolean("success")) {
         return body
      }

      return JSONObject()
   }

   override fun extractProductsFromCurrentPage() {


      log("Página $currentPage")

      val apiToken: String = fetchApiToken()

      val body = fetchData(apiToken, currentPage)

      if (totalProducts == 0) {
         val paginator = body.optJSONObject("paginator")

         totalProducts = paginator.optInt("total_items")
      }

      val data = body.optJSONObject("data")

      val products = data.optJSONArray("produtos")

      for (e in products) {
         val product = e as JSONObject
         val internalPid = product.optString("id")
         val internalId = product.optString("produto_id")
         val link = product.optString("link")
         val urlProduct = "http://www.rexdelivery.com.br/produtos/detalhe/$internalId/${link}"

         saveDataProduct(internalId, internalPid, urlProduct)
         log("Position: $position - InternalId: $internalId - InternalPid: $internalPid - Url: $urlProduct")
         if (arrayProducts.size == productsLimit) break
      }
      log("Finalizando Crawler de produtos da página " + currentPage + " - até agora " + arrayProducts.size + " produtos crawleados")
   }

}
