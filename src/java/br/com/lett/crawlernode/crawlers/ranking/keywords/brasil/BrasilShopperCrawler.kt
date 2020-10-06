package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.Logging
import org.apache.http.impl.cookie.BasicClientCookie
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

   override fun processBeforeFetch() {
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://shopper.com.br", ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session)

         log("waiting home page")

         webdriver.waitForElement("button.login", 25)

         webdriver.clickOnElementViaJavascript("button.login", 2000)

         webdriver.waitForElement(".access-login input[name=email]", 25)

         webdriver.sendToInput(".access-login input[name=email]", login, 100)

         webdriver.sendToInput(".access-login input[name=senha]", password, 100)

         log("submit login")
         webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 2000)

         cookies = webdriver.driver.manage().cookies.map {
            BasicClientCookie(it.name, it.value)
         }

      } catch (e: Exception) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e))
      }
   }

   private fun requestProducts(): Document {

      val url = "https://shopper.com.br/shop/busca?q=${keywordEncoded}"

      val headers: MutableMap<String, String> = HashMap()

      headers["authority"] = "shopper.com.br"
      headers["accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
      headers["sec-fetch-dest"] = "document"
      headers["referer"] = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"
//      headers["cookie"] = "sessionid=okawtmk320xtcg6w3f879pnl3oaueclw;"
      headers["cookie"] = cookies.filter {
         it.name == "sessionid" || it.name == "csrftoken"
      }.joinToString(";") {
         "${it.name}=${it.value}"
      }

      val request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build()

      return Jsoup.parse(dataFetcher[session, request].body)
   }

   override fun extractProductsFromCurrentPage() {

      currentDoc = requestProducts()

      val products = currentDoc.select(".prod-item[data-produto]")

      for (product in products) {

         val internalId = product.attr("data-produto")

         val name = product.selectFirst(".prod-name")?.text() ?: ""

         if (internalId.isNotEmpty()) {
            val productUrl = "https://shopper.com.br/shop/busca?q=${URLEncoder.encode(name, "UTF-8")}"
            saveDataProduct(internalId, internalId, productUrl)
            log(">>> productId: $internalId - url: $productUrl - name: $name")
         }
      }
   }

   override fun hasNextPage(): Boolean {
      return false
   }
}
