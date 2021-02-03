package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toJson
import org.apache.http.cookie.Cookie
import org.json.JSONObject

/**
 * Date: 28/01/21
 *
 * @author Fellype Layunne
 *
 */
abstract class AtacadaoCrawlerRanking(session: Session) : CrawlerRankingKeywords(session) {

   init {
      fetchMode = FetchMode.FETCHER
   }

   abstract fun getCityId(): String

   private fun getCookies(dataFetcher: DataFetcher, session: Session): List<Cookie> {
      return br.com.lett.crawlernode.crawlers.extractionutils.core.AtacadaoCrawler.getCookies(dataFetcher, session)
   }

   private fun setLocation(cityId: String, dataFetcher: DataFetcher, session: Session, cookies: List<Cookie>) {
      return br.com.lett.crawlernode.crawlers.extractionutils.core.AtacadaoCrawler.setLocation(cityId, dataFetcher, session, cookies)
   }

   override fun processBeforeFetch() {

      this.cookies = getCookies(this.dataFetcher, this.session)

      setLocation(getCityId(), this.dataFetcher, this.session, this.cookies)
   }

   private fun fetchProducts(): JSONObject {

      val url = "https://www.atacadao.com.br/catalogo/search/?q=${getKeyword()}&page=${currentPage}&order_by=-relevance"

      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
      headers["Cookie"] = CommonMethods.cookiesToString(cookies)

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
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

            val productUrlPath = product.optString("url")

            val productUrl = "https://www.atacadao.com.br$productUrlPath"

            saveDataProduct(internalId, internalId, productUrl)
            log("Position: $position - InternalId: $internalId - InternalPid: $internalId - Url: $productUrl")
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

}
