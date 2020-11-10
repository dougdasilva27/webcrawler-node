package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

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

      val request = Request.RequestBuilder.create().setUrl(url).build()

      val response =  dataFetcher.get(session, request)

      cookies = response.cookies

      return response.body.toJson()
   }
}
