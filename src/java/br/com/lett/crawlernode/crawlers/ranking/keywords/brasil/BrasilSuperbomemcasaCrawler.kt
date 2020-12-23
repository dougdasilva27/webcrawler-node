package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject

/**
 * Date: 21/12/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilSuperbomemcasaCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private fun requestGet(path: String): JSONObject {
      val url = "https://sb.superbomemcasa.com.br/${path}"

      val headers = HashMap<String, String>()

      headers["Accept"] = "*/*"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36"
      headers["Referer"] = "https://superbomemcasa.com.br/"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toJson()
   }

   private fun requestJsonId(): JSONObject {

      val path = "rest/default/V1/search" +
         "?searchCriteria[currentPage]=${currentPage}" +
         "&searchCriteria[requestName]=quick_search_container" +
         "&searchCriteria[filterGroups][0][filters][0][field]=search_term" +
         "&searchCriteria[filterGroups][0][filters][0][conditionType]=" +
         "&searchCriteria[filterGroups][0][filters][0][value]=${keywordEncoded}" +
         "&searchCriteria[filterGroups][1][filters][0][field]=mostrar" +
         "&searchCriteria[filterGroups][1][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][1][filters][0][value]=0" +
         "&searchCriteria[filterGroups][2][filters][0][field]=status" +
         "&searchCriteria[filterGroups][2][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][2][filters][0][value]=1"

      return requestGet(path)
   }

   private fun requestProducts(ids: List<String>): JSONObject {

      val path = "rest/default/V1/products/" +
         "?searchCriteria[currentPage]=1&searchCriteria[pageSize]=${ids.size}" +
         "&searchCriteria[filterGroups][0][filters][0][field]=entity_id" +
         "&searchCriteria[filterGroups][0][filters][0][conditionType]=in" +
         "&searchCriteria[filterGroups][0][filters][0][value]=${ids.joinToString(",")}" +
         "&searchCriteria[filterGroups][1][filters][0][field]=mostrar" +
         "&searchCriteria[filterGroups][1][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][1][filters][0][value]=0" +
         "&searchCriteria[filterGroups][2][filters][0][field]=status" +
         "&searchCriteria[filterGroups][2][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][2][filters][0][value]=1"

      return requestGet(path)
   }

   override fun extractProductsFromCurrentPage() {
      val jsonIds = requestJsonId()

      val ids = mutableListOf<String>()
      JSONUtils.getJSONArrayValue(jsonIds, "items").forEach {
         if (it is JSONObject) {
            ids.add(it.optString("id"))
         }
      }

      val jsonProducts = requestProducts(ids)

      val products = JSONUtils.getJSONArrayValue(jsonProducts, "items")

      val productsMap = HashMap<String, JSONObject>()

      JSONUtils.getJSONArrayValue(jsonProducts, "items").forEach {
         if (it is JSONObject) {
            productsMap[it.optString("id")] = it
         }
      }

      totalProducts = products.length()

      for (internalId in ids) {
         val product = productsMap[internalId]
         if (product != null) {
            val internalPid = product.optString("sku")
            val jsonAtt = JSONUtils.getJSONArrayValue(product, "custom_attributes")
            val pathUrl = br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilSuperbomemcasaCrawler.scrapAttributes(jsonAtt, "url_key")

            if (internalPid.isNotEmpty()) {
               val productUrl = "https://superbomemcasa.com.br/${pathUrl}.${internalPid}p"
               saveDataProduct(internalId, internalPid, productUrl)
               log(">>> productId: $internalId - internalPid $internalPid - url: $productUrl")
            }
         }
      }
   }

   override fun hasNextPage(): Boolean {
      return false
   }
}
