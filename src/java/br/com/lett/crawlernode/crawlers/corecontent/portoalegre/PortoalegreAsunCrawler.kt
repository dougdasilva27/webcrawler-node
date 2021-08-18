package br.com.lett.crawlernode.crawlers.corecontent.portoalegre

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher
import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilMartinsCrawler
import br.com.lett.crawlernode.exceptions.AuthenticationException
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import com.google.common.collect.Sets
import models.Offer
import models.Offers
import models.pricing.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

class PortoalegreAsunCrawler constructor(session: Session?) : Crawler(session) {


   companion object{
      val SELLER_FULL_NAME = "Asun"
      var cards: Set<String> = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString())
   }

   override fun fetch(): Any {
      return try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.originalURL, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session)
         Logging.printLogDebug(logger, session, "awaiting set locate")
         waitForElement(webdriver.driver, ".form-group input")
         val cep = webdriver.driver.findElement(By.cssSelector(".form-group input"))
         cep.sendKeys("91740-001")
         webdriver.waitLoad(2000)
         BrasilMartinsCrawler.waitForElement(webdriver.driver, ".form-group button")
         val login = webdriver.driver.findElement(By.cssSelector(".form-group button"))
         webdriver.clickOnElementViaJavascript(login)
         webdriver.waitLoad(2000)
         val delivery = webdriver.driver.findElement(By.cssSelector(".method-delivery button"))
         webdriver.clickOnElementViaJavascript(delivery)
         webdriver.waitLoad(5000)
         Logging.printLogDebug(logger, session, "awaiting product page")

         val doc = Jsoup.parse(webdriver.currentPageSource)
         if(doc.selectFirst("div.pLocModal") != null){
            throw AuthenticationException("Failed to set location")
         }

         return doc
      } catch (e: Exception) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e))
      }
   }

   fun waitForElement(driver: WebDriver?, cssSelector: String?) {
      val wait = WebDriverWait(driver, 20)
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)))
   }

   override fun extractInformation(document: Document?): MutableList<Product> {
      val products: MutableList<Product> = ArrayList()

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.originalURL)
         val internalId: String = CrawlerUtils.scrapStringSimpleInfo(document, ".stock", true)?.replace("[^0-9]".toRegex(), "")!!.trim();
         val internalPid: String = internalId
         val name: String? = document?.selectFirst(".details strong[itemprop=\"name\"]")?.text()
         val categories: CategoryCollection = CrawlerUtils.crawlCategories(document, ".row.bcrumb")

         val images = crawlImages(document)
         val primaryImage: String? = if (images.size >0) images.removeAt(0) else null
         val secondaryImages: MutableList<String> = images

         val availableToBuy: Boolean = true
         val offers = if (availableToBuy) scrapOffer(document, internalId) else Offers()

         // Creating the product
         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .build()
         products.add(product)
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }

      return products


   }

   private fun isProductPage(doc: Document?): Boolean {
      return doc?.selectFirst("div[itemscope=itemscope] > div.details") != null;
   }

   private fun crawlImages(document: Document?): MutableList<String> {

      var images : MutableList<String> = mutableListOf()
         document?.select(".images .content .thumbs a")?.forEach {
            images.add(it.select("img").attr("src").replace("img_50_","img_500_")
            )
         }

      return images;
   }

   private fun scrapOffer(doc: Document?, internalId: String): Offers {

      val offers = Offers();
      val pricing = scrapPricing(doc);
      //Site hasn't any sale


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;


   }

   private fun scrapPricing(doc: Document?): Pricing {
      val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".details span[itemprop=\"price\"]", null, true, ',', session)
      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".details .price-normal", null, true, ',', session)
      val creditCards: CreditCards = scrapCreditCards(spotlightPrice)

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build()
   }

   private fun scrapCreditCards(spotlightPrice: Double?): CreditCards {
      val creditCards = CreditCards()

      val installments = Installments()
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build())


      for (card in cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build())
      }

      return creditCards
   }

}
