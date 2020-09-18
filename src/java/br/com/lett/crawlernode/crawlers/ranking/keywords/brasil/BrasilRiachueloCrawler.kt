package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import com.google.gson.JsonObject
import org.json.JSONObject

class BrasilRiachueloCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 24
      fetchMode = FetchMode.FETCHER
   }

   override fun fetchJSONObject(url: String): JSONObject? {
      val headers = mapOf(
         "accept" to "*/*",
         "accept-encoding" to "no",
         "connection" to "keep-alive"
      )
      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).setProxyservice(
            listOf(
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.STORM_RESIDENTIAL_US,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NO_PROXY
            )
         ).build()

      return dataFetcher.get(session, request)?.body?.toJson()
   }

   override fun extractProductsFromCurrentPage() {

      val url = "https://recs.richrelevance.com/rrserver/api/find/v1/e20fd45b1e19a8c6?lang=pt&log=true&facetDepth=5&placement=search_page.find&query=$keywordEncoded&rows=${pageSize}&start=${(currentPage-1)*pageSize}"

      val jsonApi = fetchJSONObject(url)
      val placement = jsonApi?.optJSONArray("placements")?.optJSONObject(0)
      if (totalProducts == 0) {
         totalProducts = placement?.optInt("numFound") ?: 0
      }
      val jsonArray = placement?.optJSONArray("docs")
      jsonArray?.forEach { elemJson ->
         if (elemJson is JSONObject) {
            val skus = elemJson.optJSONArray("sku_list")
            val urlProduct = "https://www.riachuelo.com.br/${elemJson.optString("linkId")}"

            if (!skus.isEmpty) {
               for (internalId in skus) {
                  log("internal $internalId - url $urlProduct")
                  saveDataProduct(internalId.toString(), null, urlProduct, position)
               }
               position++
            }
            else {
               log("internal xxxxxx - url $urlProduct")
               saveDataProduct(null, null, urlProduct)
            }
         }
      }
   }

}
