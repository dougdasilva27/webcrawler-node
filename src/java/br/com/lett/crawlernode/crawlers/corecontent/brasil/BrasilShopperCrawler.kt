package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
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
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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

      webdriver = DynamicDataFetcher.fetchPageWebdriver("https://shopper.com.br", ProxyCollection.BUY_HAPROXY, session)

      log("waiting home page")

      webdriver.waitForElement("button.login", 40)

      webdriver.clickOnElementViaJavascript("button.login", 2000)

      webdriver.waitForElement(".access-login input[name=email]", 40)

      webdriver.sendToInput(".access-login input[name=email]", login, 100)

      webdriver.sendToInput(".access-login input[name=senha]", password, 100)

      log("submit login")
      webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 2000)

      cookies = webdriver.driver.manage().cookies.map {
         BasicClientCookie(it.name, it.value)
      }

      return requestProduct()
   }

   private fun requestProduct(): Document {

      val url = session.originalURL

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

   //pattern: https://shopper.com.br/shop/busca?q=ENCODED%20NAME
   private fun productNameByURL(): String {
      return HttpUrl.parse(session.originalURL)?.queryParameter("q") ?: ""
   }

   private fun scrapProductDiv(doc: Document): Element? {
      val products = doc.select(".prod-item")

      for (productDiv in products) {
         val name = productDiv?.selectFirst(".prod-name")?.text() ?: ""

         if (isProductPage(name)) {
            return productDiv
         }
      }
      return null
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      log("scrap product")

      val productDetails = scrapProductDiv(doc)

      if (productDetails == null) {
         log("Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val name = productDetails.selectFirst(".prod-name")?.text() ?: ""

      val internalId = productDetails.selectFirst(".prod-item[data-produto]")?.attr("data-produto")

      val primaryImage = productDetails.selectFirst(".prod-photo img")?.attr("data-src")

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
