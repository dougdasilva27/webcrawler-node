package br.com.lett.crawlernode.crawlers.ranking.keywords.belgium

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.Cookie
import org.json.JSONObject

/**
 * Date: 30/07/20
 *
 * @author Fellype Layunne
 *
 */
class BelgiumDelhaizeCrawler(session: Session) : CrawlerRankingKeywords(session) {

   init {
      pageSize = 100
   }

   override fun extractProductsFromCurrentPage() {

      val result = requestApi()

      if (this.totalProducts == 0) {
         setTotalProductsAndPageSize(result)
      }

      val products = result.optJSONArray("results")

      for (it in products) {

         val product = it as JSONObject

         val internalId = product.optString("code")

         val productUrl = "https://www.delhaize.be${product.optString("url")}"

         saveDataProduct(internalId, internalId, productUrl)
         log("Position: " + position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl)
      }
   }

   private fun requestApi() : JSONObject {

      val url = "https://www.delhaize.be/search/products/loadMore" +
         "?text=$keywordEncoded" +
         "&pageSize=$pageSize" +
         "&pageNumber=${currentPage-1}" +
         "&sort=relevance"

      val headers: MutableMap<String, String> = HashMap()

      headers["cookie"] = "groceryCookieLang=fr;"

      val request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build()

      return CrawlerUtils.stringToJson(dataFetcher[session, request].body)
   }

   private fun setTotalProductsAndPageSize(search: JSONObject) {

      val pagination = search.optJSONObject("pagination")

      totalProducts = pagination.optInt("totalNumberOfResults")
   }
}
