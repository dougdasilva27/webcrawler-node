package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.LettProxy
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

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

   override fun fetch(): Any {
      webdriver = DynamicDataFetcher.fetchPageWebdriver("https://shopper.com.br", ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session)

      webdriver.waitLoad(20000)

      webdriver.clickOnElementViaJavascript("button.login", 5000)

      webdriver.sendToInput(".access-login input[name=email]", login, 500)
      webdriver.sendToInput(".access-login input[name=senha]", password, 500)

      webdriver.clickOnElementViaJavascript(".access-login button[type=submit]", 25000)

      val internalId = getProductIdFromURL()

      webdriver.findAndClick("div[data-produto=\"${internalId}\"]", 5000)

      return Jsoup.parse(webdriver.currentPageSource)
   }

   private fun getProductIdFromURL(): String {
      val split = session.originalURL.split("/")

      return if (split.isNotEmpty()) {
         split.last()
      } else {
         ""
      }
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val productDetails = doc.selectFirst(".mfp-content #popupProduto")

      val name = productDetails.selectFirst("prod-nome.")?.text()

      val internalId = doc.selectFirst(".prod-item[data-produto]")?.attr("data-produto")

      val primaryImage = doc.selectFirst(".prod-foto img")?.attr("src")

      val available = doc.selectFirst(".prod-buttons .add-text") != null

      val offers = if (available) scrapOffers(doc) else Offers()

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setOffers(offers)
         .setRatingReviews(null)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prod-economia span", null, false, ',', session)

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
         Card.SOROCRED,
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

   private fun isProductPage(document: Document): Boolean {
      return document.selectFirst(".mfp-content #popupProduto") != null
   }
}
