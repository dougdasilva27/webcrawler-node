package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import models.Offer
import models.Offers
import models.pricing.Pricing
import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.TimeoutException

/**
 * Date: 28/09/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilShopperCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Shopper"
   }

   private val login: String = "kennedybarcelos@lett.digital"
   private val password: String = "K99168938690"

   override fun fetch(): Document {
      val name = productNameByURL().toLowerCase()

      if (name.isEmpty()) {
         return Document(session.originalURL)
      }

      webdriver = DynamicDataFetcher.fetchPageWebdriver("https://shopper.com.br", ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session)

      log("waiting home page")

      webdriver.waitForElement("button.login", 25)

      webdriver.clickOnElementViaJavascript("button.login", 1000)

      webdriver.waitForElement(".access-login input[name=email]", 25)

      webdriver.sendToInput(".access-login input[name=email]", login, 100)

      webdriver.sendToInput(".access-login input[name=senha]", password, 100)

      log("submit login")
      webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 2000)

      webdriver.loadUrl(session.originalURL, 25000)

      try {
         log("wait for: product div")
         webdriver.waitForElement("div[data-produto]", 60)
      } catch (e: TimeoutException) {
         return Jsoup.parse(webdriver.currentPageSource)
      }

      return Jsoup.parse(webdriver.currentPageSource)
   }

   //pattern: https://shopper.com.br/shop/busca?q=ENCODED%20NAME
   private fun productNameByURL(): String {
      return HttpUrl.parse(session.originalURL)?.queryParameter("q") ?: ""
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      log("scrap product")

      val productDetails = doc.selectFirst(".prod-item")

      val name = productDetails?.selectFirst(".prod-name")?.text() ?: ""

      if (!isProductPage(name)) {
         log("Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val internalId = productDetails.selectFirst(".prod-item[data-produto]")?.attr("data-produto")

      val primaryImage = productDetails.selectFirst(".prod-foto img")?.attr("src")

      val available = productDetails.selectFirst(".prod-buttons .add-text") != null

      val offers = if (available) scrapOffers(doc) else Offers()

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setOffers(offers)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prod-prices p", null, false, ',', session)

      val bankSlip = price.toBankSlip()

      val creditCards = listOf(
         Card.MASTERCARD,
         Card.VISA,
         Card.ELO,
         Card.AMEX,
         Card.DINERS,
         Card.HIPERCARD,
         Card.JCB,
         Card.CABAL,
         Card.SOROCRED
      ).toCreditCards(price)

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .setCreditCards(creditCards)
                  .setBankSlip(bankSlip)
                  .build()
            )
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .build()
      )

      return offers
   }

   fun log(message: String) {
      Logging.printLogDebug(logger, session, message)
   }

   private fun isProductPage(nameFromHtml: String): Boolean {

      val nameFromURL = productNameByURL()

      return nameFromHtml.toLowerCase() == nameFromURL.toLowerCase()
   }
}
