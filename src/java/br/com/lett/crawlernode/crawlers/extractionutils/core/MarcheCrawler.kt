package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.nodes.Document

abstract class MarcheCrawler(session: Session) : Crawler(session) {

   private val home = "https://www.marche.com.br/"

   abstract fun getCEP(): String
   abstract fun getSellerName(): String

   override fun handleCookiesBeforeFetch() {
      cookies.add(BasicClientCookie("user_zip_code", getCEP()))
   }

   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith(home)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)
      val products = mutableListOf<Product>()

      if (!isProductPage(doc)) {
         return products
      }

      val internalId = scrapInternalId(doc)
      val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info .product-name", false)
      val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-wrapper li span")
      val offers = scrapOffers(doc)
      val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-image img", "src")

      products.add(
         ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .build()
      )

      return products
   }

   private fun scrapInternalId(doc: Document): String? {
      var internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=\"product_id\"][value]", "value")

      if (internalId.isNullOrEmpty()) {
         val productJSON = JSONUtils.stringToJson(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-info > div[data-json]", "data-json"))
         if (!productJSON.isEmpty) {
            internalId = productJSON.optString("product_id")
         }
      }

      return internalId
   }

   fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".product-info .product-name") != null
   }

   private fun scrapOffers(doc: Document): Offers {
      var sale: String? = ""

      val offers = Offers()

      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price span del", null, false, ',', session)

      val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price b", null, false, ',', session)

      val bankSlip = price.toBankSlip()

      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX).toCreditCards(price)

      val sales: MutableList<String> = mutableListOf();

      if (priceFrom != null && priceFrom!! > price) {
         val value = Math.round((price / priceFrom!! - 1.0) * 100.0).toInt()
         sale = Integer.toString(value)
      }
      sales.add(sale.toString());

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setCreditCards(creditCards)
                  .setSpotlightPrice(price)
                  .setPriceFrom(priceFrom)
                  .setBankSlip(bankSlip)
                  .build()
            )
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(getSellerName())
            .setSales(sales)
            .build()
      )

      return offers
   }
}
