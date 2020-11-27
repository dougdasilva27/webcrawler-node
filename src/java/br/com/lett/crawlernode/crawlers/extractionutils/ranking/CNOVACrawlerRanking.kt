package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject
import java.util.*

/**
 * Date: 09/11/20
 *
 * @author Fellype Layunne
 */
abstract class CNOVACrawlerRanking(session: Session?) : CrawlerRankingKeywords(session) {

   protected abstract fun getApiKey(): String

   public override fun extractProductsFromCurrentPage() {
      pageSize = 100

      val search = fetchProductsFromAPI()

      val products = JSONUtils.getJSONArrayValue(search, "products")

      if (this.totalProducts == 0) {
         this.totalProducts = search.optInt("size", 0)
      }

      for (i in 0 until products.length()) {
         val product = products.optJSONObject(i)
         if (product != null) {

            val internalPid = product.optString("id", null)
            val productUrl = JSONUtils.getStringValue(product, "url")?.split("\\?")?.get(0)

            val variations = JSONUtils.getJSONArrayValue(product, "skus")

            if (!variations.isEmpty && internalPid != null) {

               var isAddPosition = true

               for (j in 0 until variations.length()) {
                  val variation = variations.optJSONObject(j)
                  if (variation != null) {

                     val sku = variation.optString("sku")

                     val internalId = "${internalPid}-${sku}"
                     if (isAddPosition) {
                        this.position++
                        isAddPosition = false
                     }

                     saveDataProduct(internalId, internalPid, productUrl, position)
                     log("Position: $position - InternalId: $internalId - InternalPid: $internalPid - Url: $productUrl")
                  }
               }


            } else if(internalPid != null || productUrl != null) {
               saveDataProduct(null, internalPid, productUrl)
               log("Position: $position - InternalId: null - InternalPid: $internalPid - Url: $productUrl")
            }

            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      }

      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} +  produtos crawleados")
   }

   private fun fetchProductsFromAPI(): JSONObject {
      val url = "https://prd-api-partner.viavarejo.com.br/api/search" +
         "?resultsPerPage=$pageSize" +
         "&terms=$keywordEncoded" +  //maquina%2Bde%2Bcafe
         "&apiKey=${getApiKey()}" +
         "&page=$currentPage"

      val headers: MutableMap<String, String> = HashMap()
      headers["accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
      headers["accept-encoding"] = "gzip, deflate, br"
      headers["accept-language"] = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6"
      headers["cache-control"] = "no-cache"
      headers["pragma"] = "no-cache"
      headers["sec-fetch-dest"] = "document"
      headers["sec-fetch-mode"] = "navigate"
      headers["sec-fetch-site"] = "none"
      headers["sec-fetch-user"] = "?1"
      headers["upgrade-insecure-requests"] = "1"
      headers["ak_bmsc"] = "DBC817405614D3D751F263F55B55A1228A7AC44DAC2100000AD4C05F24FBCD05~plWrjWUWgdXlFj2JvlQi2GxrJoTrHhd3KXQ7P6HmMQRj0r2higFJ+DGPeDauPrO9Pi+RivzTJiZ+GmcnKdY3P+Tx1ymEReA+p2mZ44U1c/plxcRhABRroUwMpWVYMQGzQJEGt9se0Tf6wuGi4RLAvV7MTRGi7qYMyVpn9G3p7zEVrGbvRY0Em6A5ZhSk/65b/8xkQEwz2tHjQIxHhdB+ZAUPu3Dw/rr5Hxog53uGYgmAk="

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setFetcheroptions(FetcherOptionsBuilder.create()
            .mustUseMovingAverage(false)
            .mustRetrieveStatistics(true)
            .build())
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            )
         ).build()

      val body: String = alternativeFetch(request).body

      return body.toJson()
   }

   private fun alternativeFetch(request: Request): Response {

      var response = JsoupDataFetcher().get(session, request)

      val statusCode = response.lastStatusCode

      if (statusCode.toString()[0] != '2' && statusCode.toString()[0] != '3' && statusCode != 404) {
         response = ApacheDataFetcher().get(session, request)
      }
     
      return response
   }
}
