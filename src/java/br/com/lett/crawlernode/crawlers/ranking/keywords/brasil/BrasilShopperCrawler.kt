package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.Logging
import org.jsoup.Jsoup

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
         webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 25000)

         currentDoc = Jsoup.parse(webdriver.currentPageSource)
      } catch (e: Exception) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e))
      }
   }

   override fun extractProductsFromCurrentPage() {

      val products = currentDoc.select(".prod-item[data-produto]")

      if (this.totalProducts == 0) {
         totalProducts = products.size
      }

      for (product in products) {

         val internalId = product.attr("data-produto")

         val productUrl = "https://shopper.com.br/shop?product=${internalId}"

         saveDataProduct(internalId, internalId, productUrl)
         log(">>> ✅ productId: $internalId url: $productUrl")
      }
   }
}
