package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher
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
import java.util.*

abstract class MarcheCrawler(session: Session) : Crawler(session) {

   private val home = "https://www.marche.com.br/"

   abstract fun getCEP(): String
   abstract fun getSellerName(): String

   override fun handleCookiesBeforeFetch() {

      this.cookies = getCookies(getCEP(), JsoupDataFetcher(), session)
   }

   companion object {
      fun getDefaultHeaders(): MutableMap<String, String> {

         val headers = mutableMapOf<String, String>()

         headers["Connection"] = "keep-alive"
         headers["Origin"] = "https://www.marche.com.br"
         headers["Referer"] = "https://www.marche.com.br/home"
         headers["Accept-Encoding"] = "gzip, deflate, br"
         headers["Accept-Language"] = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"


         return headers
      }

      fun getCookies(cep: String, dataFetcher: DataFetcher, session: Session): List<Cookie> {
         val url = "https://www.marche.com.br/deliverable_zip_codes/set_zipcode"

         val headers = getDefaultHeaders()
         headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
         headers["Content-Type"] = "application/x-www-form-urlencoded"
         headers["Cookie"] =
            "ahoy_visitor=98119b1b-68bf-4254-bf79-74345013b02c; ahoy_visit=f22ff97b-292c-43e4-8b71-efa0ae681c63; device=NTM0ODQ4NTQ%3D--636d3d43282011f6388820725fabbbe4f728aa20; GTMUtmTimestamp=1661867943791; GTMUtmSource=(direct); GTMUtmMedium=(none); _gcl_au=1.1.1089714351.1661867944; __kdtv=t%3D1654005717251%3Bi%3D83f5cd8b0052e25e9a3f2833c68552b931f954da; _kdt=%7B%22t%22%3A1654005717251%2C%22i%22%3A%2283f5cd8b0052e25e9a3f2833c68552b931f954da%22%7D; _ga=GA1.3.441391984.1661867945; _gid=GA1.3.1100954808.1661867945; _gat_UA-144859923-1=1; back_location=Imh0dHBzOi8vd3d3Lm1hcmNoZS5jb20uYnIvaG9tZSI%3D--1a9d07e7dadba515026ce73bce553fcb182a7db5; _fbp=fb.2.1661867944722.695973195; _st_marche_ecomm_session=UmhYT09xRHpnUWRxMlhyVTBRaEYvc2ozdVRrVzl5eHRHTDRCQ0szYkFtTU9lZnhCQXN1cmtJQUxubG9WdDJGUHBMQjNPd3BRaStVaVFVRC9ZTVRBMjJ6b0ZlQnY1d3FhTmhmTjNVS0Y0TmRUTWgyeE5mZENBV21OSm1MenJuV0M0RXpvN3BHREczakw2UDJyUWphL2VnPT0tLXhKemVXQXJPcUhpSklvamhMRTIwVEE9PQ%3D%3D--6dafd0cfece5b93a43b3045e9a8e204ed69df3cb; ahoy_events=%5B%5D; GTMGAHitCounter_UA-144859923-1=2"


         val payload = "utf8=%E2%9C%93" +
            "&authenticity_token=52DYTgHGNtSiAt5BWGoswoQv3Ir8HveTx7HDOBZgCqa6%2B7zXF7Qm0dd6%2FXEbWlbBWfWd3jFxuDJiIMH5ofNtMQ%3D%3D" +
            "&zip_code%5Bcode%5D=${cep}&commit=Enviar"

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setPayload(payload)
            .setProxyservice(
               Arrays.asList(
                  ProxyCollection.BUY,
                  ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
               )
            )
            .setFollowRedirects(false)
            .setSendUserAgent(true)
            .build()

         val response = dataFetcher.post(session, request)

         return response.cookies
      }
   }

   override fun fetch(): Document {

      val headers = getDefaultHeaders()
      headers["Accept"] = "text/html, application/xhtml+xml"

      val request = Request.RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers)
         .setCookies(this.cookies)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
            )
         )
         .setSendUserAgent(true)
         .build()

      val response = ApacheDataFetcher().get(session, request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }


   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith(home)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

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

      val offers = Offers()

      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price span del", null, false, ',', session)

      val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price b", null, false, ',', session)

      val bankSlip = price.toBankSlip()

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
            .setSellerFullName(getSellerName())
            .build()
      )

      return offers
   }
}
