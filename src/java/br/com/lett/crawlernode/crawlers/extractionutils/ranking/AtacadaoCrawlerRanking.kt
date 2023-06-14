package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher
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

   private fun getNeighborhood(): String? {
      return session.options.optString("cb_user_neighborhood")
   }

   fun setCookies(): List<Cookie> {
      this.cookies.add(BasicClientCookie("cb_user_type", session.options.optString("cb_user_type")))
      this.cookies.add(BasicClientCookie("cb_user_city_id", session.options.optString("cb_user_city_id")))

      return this.cookies;
   }

   private fun fetchProducts(): JSONObject {

      var url = "https://algolia.cotabest.com.br/catalogo?displayFacets=true&queryString=${getKeyword()}&page=${currentPage - 1}&order_by=-relevance"
      val regionIds = requestRegionIds()
      if (regionIds != null) {
         url = "https://algolia.cotabest.com.br/catalogo?displayFacets=true&queryString=${getKeyword()}&commaSeparatedRegionIds=${regionIds}&page=${currentPage - 1}&order_by=-relevance"
      }
      setCookies()

      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["x-requested-with"] = "XMLHttpRequest"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
      headers["cookie"] = CommonMethods.cookiesToString(cookies)

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(
            listOf(
               ProxyCollection.BUY,
               ProxyCollection.LUMINATI_SERVER_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            )
         )
         .build()

      val response = CrawlerUtils.retryRequest(request, session, ApacheDataFetcher(), true);
      return response.body.toJson()
   }

   private fun requestRegionIds(): String? {
      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["Connection"] = "keep-alive"
      headers["Origin"] = "https://www.atacadao.com.br"
      headers["Referer"] = "https://www.atacadao.com.br/"

      val request = Request.RequestBuilder.create()
         .setUrl("https://apis.cotabest.com.br/logistic/logistics/neighborhoods/${getNeighborhood()}/PJ/available-logistics")
         .setHeaders(headers)
         .setProxyservice(
            listOf(
               ProxyCollection.BUY,
               ProxyCollection.LUMINATI_SERVER_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            )
         )
         .build()

      val response = CrawlerUtils.retryRequest(request, session, ApacheDataFetcher(), true);
      val result = response.body
      return result.replace(Regex("[\\[\\]]"), "")
   }

   override fun extractProductsFromCurrentPage() {
      pageSize = 20

      val data = fetchProducts()
      val products = JSONUtils.getJSONArrayValue(data, "results")

      for (product in products) {
         if (product is JSONObject) {
            val internalId = product.optString("id", null)
            val productUrl = CrawlerUtils.completeUrl(product.optString("slug"), "https", "www.atacadao.com.br");
            val name = product.optString("name")
            val imageUrl = getImage(product)
            val price = getPrice(product)
            val isAvailable = price != null

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
      val providers = product.optJSONArray("providers")
      var minimumPrice: Double = Double.MAX_VALUE

      for (i in 0 until providers.length()) {
         val provider = providers.getJSONObject(i)
         val prices = provider.getJSONArray("prices")

         for (j in 0 until prices.length()) {
            val price = prices.getJSONObject(j).getDouble("price")

            if (price < minimumPrice) {
               minimumPrice = price
            }
         }
      }

      return CommonMethods.doublePriceToIntegerPrice(minimumPrice, null)
   }

   private fun getImage(product: JSONObject): String? {
      val imageSlug = product.optString("photo")
      if (imageSlug != null) {
         return "https://media.cotabest.com.br/media/sku/$imageSlug"
      }

      return null
   }

   override fun hasNextPage(): Boolean {
      return true
   }
}
