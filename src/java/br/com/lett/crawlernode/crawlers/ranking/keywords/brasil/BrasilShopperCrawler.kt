package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import com.google.gson.JsonObject
import org.apache.http.impl.cookie.BasicClientCookie
import org.eclipse.jetty.util.ajax.JSON
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

/**
 * Date: 01/10/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilShopperCrawler(session: Session) : CrawlerRankingKeywords(session) {

   private val login: String = "kennedybarcelos@lett.digital"
   private val password: String = "K99168938690"
   private var token: String = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjdXN0b21lcklkIjoyOTIyNjAsImRldmljZVVVSUQiOiIzYTc1YjdkNy1mMDhmLTQ4ZmEtOGM5Mi04OTliZjNkZmE1Y2IiLCJpYXQiOjE2MjQ2MjMzODl9.KXv2rXCKSkwERiGywoP6sI5HB_mSgp_sdsjN79qq338";

   override fun processBeforeFetch() {
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://shopper.com.br", ProxyCollection.BUY_HAPROXY, session)


         log("waiting home page")

         webdriver.waitForElement("button.login", 30)

         webdriver.clickOnElementViaJavascript("button.login", 5000)

         webdriver.waitForElement(".access-login input[name=email]", 30)

         webdriver.sendToInput(".access-login input[name=email]", login, 2000)

         webdriver.sendToInput(".access-login input[name=senha]", password, 2000)

         log("submit login")
         webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 20000)


         webdriver.waitForElement("#home", 240)

         cookies = webdriver.driver.manage().cookies.map {
            BasicClientCookie(it.name, it.value)
         }


         var tokenShopper = this.cookies.first { it.name == "shopper_token" }.value

         if (!tokenShopper.isEmpty()) {
            token = tokenShopper
         }

      } catch (e: Exception) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e))
         Logging.printLogWarn(logger,"login não realizado")
      }
   }

   private fun requestProducts(): JSONObject {

      val url = "https://siteapi.shopper.com.br/catalog/search?query=${keywordEncoded}"

      val headers: MutableMap<String, String> = HashMap()

      headers["authorization"] = "Bearer $token"
      headers["accept"] = "application/json, text/plain, */*"

      val request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build()

      return  CrawlerUtils.stringToJSONObject(dataFetcher[session, request].body)
   }

   override fun extractProductsFromCurrentPage() {

      val productsApi = requestProducts()

      val products = productsApi.optJSONArray("products")

      for (p in products) {
         val product = p as JSONObject
         val internalId = product.optInt("id").toString()

         val productUrl = "https://shopper.com.br/?id="+internalId

            saveDataProduct(internalId, internalId, productUrl)
         this.log("internalId: $internalId - internalPid: $internalId - url: $productUrl")
      }
   }

   override fun hasNextPage(): Boolean {
      return false
   }
}
