package br.com.lett.crawlernode.crawlers.corecontent.argentina

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
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
import org.apache.http.cookie.Cookie
import org.jsoup.nodes.Document

/**
 * Date: 26/01/21
 *
 * @author Fellype Layunne
 *
 */
class ArgentinaLareinaCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.JSOUP
   }

   companion object {
      const val SELLER_NAME: String = "La Reina"

      fun getCookies(dataFetcher: DataFetcher, session: Session): List<Cookie> {
         val url = "https://www.lareinaonline.com.ar/index.asp"

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setFollowRedirects(false)
            .build()

         val response = dataFetcher.get(session, request)

         return response.cookies
      }
   }

   private fun scrapInternalIdFromUrl(): String? {
      return CommonMethods.getQueryParamFromUrl(session.originalURL, "Pr")
   }

   override fun handleCookiesBeforeFetch() {
      cookies = getCookies(dataFetcher, session)
   }

   override fun fetch(): Document {

      val internalId = scrapInternalIdFromUrl() ?: return Document(session.originalURL)

      val url = "https://www.lareinaonline.com.ar/Detalle.asp?Pr=${internalId}&P="

      val headers = HashMap<String, String>()

      headers["Cookie"] = CommonMethods.cookiesToString(cookies)
      headers["Accept"] = "*/*"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = doc.selectFirst(".DetallDesc > b")?.text()
      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".Detquantity input[id]", "id")?.substringAfter("c")

      var primaryImage: String? = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".DetallIzq .tile[data-image]", "data-image")

      if (primaryImage != null) {
         primaryImage = "https://www.lareinaonline.com.ar/${primaryImage}"
      }

      val offers = scrapOffers(doc)

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

      var priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.DetallPrec > div", null, false, ',', session)

      var spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".DetallDesc .DetallPrec b", null, false, ',', session) ?: return offers

      if (spotlightPrice == priceFrom) {
         spotlightPrice = priceFrom
         priceFrom = null
      }

      val bankSlip = spotlightPrice.toBankSlip()

      val creditCards = listOf(
         Card.MASTERCARD,
         Card.VISA,
         Card.DINERS,
         Card.AMEX,
      ).toCreditCards(spotlightPrice)

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setPriceFrom(priceFrom)
                  .setSpotlightPrice(spotlightPrice)
                  .setCreditCards(creditCards)
                  .setBankSlip(bankSlip)
                  .build()
            )
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .setSales(listOf())
            .build()
      )

      return offers
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".DetallDesc > b") != null
   }

}
