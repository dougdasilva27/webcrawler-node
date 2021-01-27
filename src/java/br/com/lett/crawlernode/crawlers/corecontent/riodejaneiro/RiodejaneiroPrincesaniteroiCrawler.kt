package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro

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
 * Date: 27/01/21
 *
 * @author Fellype Layunne
 *
 */

class RiodejaneiroPrincesaniteroiCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.FETCHER
   }

   companion object {
      const val SELLER_NAME: String = "Princesa"
      const val REGION: String = "2"

      fun getCookiesInRegion(region: String, dataFetcher: DataFetcher, session: Session): List<Cookie> {
         val url = "https://www.princesasupermercados.com.br/regiao/entrar"

         val payload = "region.id=${region}"

         val headers = HashMap<String, String>()
         headers["Content-Type"] = "application/x-www-form-urlencoded"
         headers["Accept"] = "*/*"

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setPayload(payload)
            .setFollowRedirects(false)
            .build()

         val response = dataFetcher.post(session, request)

         return response.cookies
      }
   }

   private fun getRegion(): String {
      return REGION
   }

   override fun handleCookiesBeforeFetch() {
      cookies = getCookiesInRegion(getRegion(), dataFetcher, session)
   }

   override fun fetch(): Document {

      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["Cookie"] = CommonMethods.cookiesToString(cookies)

      val request = Request.RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = CrawlerUtils.scrapStringSimpleInfo(doc, "#p-product-view .pages-space-content header > h1", false)

      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#p-view-right-buy input[name=\"product.id\"][value]", "value")

      val primaryImage: String? = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#p-view-left-main-image img[src]", "src")

      val offers = scrapOffers(doc)

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setOffers(offers)
         .build()

      return mutableListOf(product)
   }

   /*
   * I didn't find products with discounts
   * I didn't find products unavailable
   */
   private fun scrapOffers(doc: Document): Offers {

      val offers = Offers()

      val spotlightPrice: Double? = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#p-view-right-price-final", null, true, ',', session)

      spotlightPrice ?: return offers

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
      return doc.selectFirst("#p-product-view .pages-space-content #p-view-content") != null
   }
}
