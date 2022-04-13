package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.apache.http.cookie.Cookie
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject

class AtacadaoCrawlerRanking(session: Session) : CrawlerRankingKeywords(session) {

   init {
         fetchMode = FetchMode.FETCHER
   }

   fun setCookies(): List<Cookie> {
      this.cookies.add(BasicClientCookie("cb_user_type", session.options.optString("cb_user_type")))
      this.cookies.add(BasicClientCookie("cb_user_city_id", session.options.optString("cb_user_city_id")))

      return this.cookies;
   }

   private fun fetchProducts(): JSONObject {

      setCookies()
      val url = "https://www.atacadao.com.br/catalogo/search/?q=${getKeyword()}&page=${currentPage}&order_by=-relevance"
      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
      headers["cookie"] = CommonMethods.cookiesToString(cookies)

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(
            listOf(
               ProxyCollection.BUY,
               ProxyCollection.LUMINATI_SERVER_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            )
         ).setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toJson()
   }

   override fun extractProductsFromCurrentPage() {
      pageSize = 20

      val data = fetchProducts()

      if (this.totalProducts == 0) {
         this.totalProducts = data.optInt("total_results", 0)
      }

      val products = JSONUtils.getJSONArrayValue(data, "results")

      for (product in products) {

         if (product is JSONObject) {
            val internalId = product.optString("pk", null)
            val productUrl = CrawlerUtils.completeUrl(product.optString("url"), "https", "www.atacadao.com.br");
            val name = product.optString("full_display")
            val imageUrl = getImage(product)
            val price = getPrice(product)
            val isAvailable = price != 0

            val productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build()

            saveDataProduct(productRanking)

            if (arrayProducts.size == productsLimit) {
               break
            }
         }
      }

      log("Finalizando Crawler de produtos da página $currentPage - até agora ${arrayProducts.size} +  produtos crawleados")

   }

   private fun getKeyword(): String {
      return keywordWithoutAccents.replace(" ", "%20")
   }

   private fun getPrice(product: JSONObject): Int? {
      val price = product.optQuery("/price/price")
      if (price != null) {
         return CommonMethods.stringPriceToIntegerPrice(price as String?, ',', 0)
      }
      return null
   }

   private fun getImage(product: JSONObject): String? {
      val imageUrl = product.optQuery("/photo_url/0")
      if (imageUrl != null) {
         return imageUrl as String?
      }
      return null
   }

}
