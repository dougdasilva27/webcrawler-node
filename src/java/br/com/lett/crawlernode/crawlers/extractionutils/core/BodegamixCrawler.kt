package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.eachText
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toDoubleComma
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.jsoup.nodes.Document

class BodegamixCrawler(session: Session) : Crawler(session) {
   init {
      config.fetcher = FetchMode.FETCHER
   }

   private fun getUser(): String? {
      return session.options.optString("user")
   }

   private fun getPass(): String? {
      return session.options.optString("pass")
   }

   override fun handleCookiesBeforeFetch() {
      if (getUser() != null && getPass() != null) {

         val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
         val request = RequestBuilder.create().setUrl("https://bodegamix.com.br/login")
            .setPayload("Username=${getUser()}&Password=${getPass()}&RememberMe=true&RememberMe=false")
            .setHeaders(headers)
            .setIgnoreStatusCode(true)
            .setFollowRedirects(false)
            .setBodyIsRequired(false)
            .build()

         val cookie = dataFetcher.post(session, request).cookies.firstOrNull { it.name == "NOPCOMMERCE.AUTH" }
         this.cookies.add(cookie)
      }
   }

   override fun extractInformation(doc: Document): MutableList<Product> {
      val products = mutableListOf<Product>()

      if (!isProductPage(doc)) {
         return products
      }

      val internalId = doc.selectFirst("#product-details-form > div").attr("data-productid")
      val name = doc.selectFirst(".product-name h1").text()
      val categories = (doc.select(".product-essential div.breadcrumb li")?.eachText(ignoreIndexes = arrayOf(0)) as ArrayList).map { it.replace(" /", "") }
      val offers = scrapOffers(doc)
      val primaryImage = doc.selectFirst(".gallery .picture img").attr("src")
      val description = doc.selectFirst(".short-description").text()

      products.add(
         ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setDescription(description)
            .setPrimaryImage(primaryImage)
            .build()
      )

      return products
   }

   fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst("form[asp-route]") != null
   }

   private fun scrapOffers(doc: Document): Offers {

      val offers = Offers()

      if (doc.selectFirst(".stock span[class=value]")?.text() != "Em estoque") {
         return offers
      }

      val priceFrom = doc.selectFirst(".old-product-price span").toDoubleComma()

      val price = doc.selectFirst("span[itemprop=price]").toDoubleComma()

      val bankSlip = price?.toBankSlip()!!

      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX).toCreditCards(price)

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
            .setSellerFullName("Bodegamix")
            .build()
      )

      return offers
   }
}
