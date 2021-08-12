package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import okhttp3.HttpUrl
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject

/**
 * Date: 28/09/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilShopperCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Shopper"
      var token: String = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJjdXN0b21lcklkIjoyOTIyNjAsImRldmljZVVVSUQiOiIzYTc1YjdkNy1mMDhmLTQ4ZmEtOGM5Mi04OTliZjNkZmE1Y2IiLCJpYXQiOjE2MjQ2MjMzODl9.KXv2rXCKSkwERiGywoP6sI5HB_mSgp_sdsjN79qq338";
   }

   //kennedybarcelos@lett.digital
   //K99168938690

   private val password = getPassword()
   private val login = getLogin()

   protected fun getPassword(): String?{
      return session.options.optString("password");
   }

   protected fun getLogin(): String?{
      return session.options.optString("login");
   }


   override fun fetch(): JSONObject? {
      val name = productIdByURL().toLowerCase()

      uptadeToken();

      return requestProduct()
   }

   private fun uptadeToken() {
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://shopper.com.br", ProxyCollection.BUY_HAPROXY, session)

         log("waiting home page")
         webdriver.waitForElement("button.login", 30)

         log("clicking on login button")
         webdriver.clickOnElementViaJavascript("button.login", 5000)

         webdriver.waitForElement(".access-login input[name=email]", 30)

         log("inserting credentials")
         webdriver.sendToInput(".access-login input[name=email]", login, 2000)

         webdriver.sendToInput(".access-login input[name=senha]", password, 2000)

         log("submit login")
         webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 20000)


         webdriver.waitForElement("#home", 240)

         cookies = webdriver.driver.manage().cookies.map {
            BasicClientCookie(it.name, it.value)
         }

         var tokenShopper = this.cookies.first { it.name == "shopper_token" }.value

         if (tokenShopper.isEmpty()) {
            token = tokenShopper
         }
      } catch (e: Exception) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e))
         Logging.printLogWarn(logger, "login não realizado")
      }
   }

   private fun requestProduct(): JSONObject? {

      val url = "https://siteapi.shopper.com.br/catalog/products/${productIdByURL()}"

      val headers: MutableMap<String, String> = HashMap()

      headers["authorization"] = "Bearer $token"

      val request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build()



      return CrawlerUtils.stringToJSONObject(dataFetcher[session, request].body)
   }

   //pattern: https://shopper.com.br/shop/busca?q=ENCODED%20NAME
   private fun productIdByURL(): String {
      return HttpUrl.parse(session.originalURL)?.queryParameter("id") ?: ""
   }


   override fun extractInformation(json: JSONObject?): MutableList<Product> {


      if (json == null) {
         log("Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val name = json.optString("name")

      val internalId = json.getInt("id").toString()

      val primaryImage = json.optString("image")

      val offers = scrapOffers(json)

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

   private fun scrapOffers(json: JSONObject): Offers {
      val offers = Offers()

      val stringPrice = json.optString("price")

      val price: Double = MathUtils.parseDoubleWithComma(stringPrice)

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

}
