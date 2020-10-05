package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.Logging
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

         webdriver.waitLoad(20000)

         webdriver.clickOnElementViaJavascript("button.login", 5000)

         webdriver.sendToInput(".access-login input[name=email]", login, 500)
         webdriver.sendToInput(".access-login input[name=senha]", password, 500)

         log("submit login")
         webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 5000)

         currentDoc = Jsoup.parse(webdriver.currentPageSource)
      } catch (e: Exception) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e))
      }
   }

   private fun requestProducts(): Document {
      webdriver.loadUrl("https://shopper.com.br/shop/busca?q=${keywordEncoded}", 20000)
      return Jsoup.parse(webdriver.currentPageSource)
   }

   override fun extractProductsFromCurrentPage() {

      currentDoc = requestProducts()

      val products = currentDoc.select(".prod-item[data-produto]")

      if (this.totalProducts == 0) {
         totalProducts = products.size
      }

      for (product in products) {

         val internalId = product.attr("data-produto")

         val name = product.selectFirst("prod-name")?.text()

         val productUrl = "https://shopper.com.br/shop/busca?q=${URLEncoder.encode(name, "UTF-8")}"

         saveDataProduct(internalId, internalId, productUrl)
         log(">>> productId: $internalId - url: $productUrl - name: $name")
      }
   }
}
