package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

abstract class MarcheCrawler(session: Session) : Crawler(session) {

   private val home = "https://www.marche.com.br/"

   abstract fun getCEP(): String
   abstract fun getSellerName(): String

   override fun handleCookiesBeforeFetch() {

      this.cookies = getCookies(getCEP(), FetcherDataFetcher(), session)
   }

   companion object {
      fun getDefaultHeaders(): MutableMap<String, String> {

         val headers = mutableMapOf<String,String>()

         headers["Connection"] = "keep-alive"
         headers["Origin"] = "https://www.marche.com.br"
         headers["Content-Type"] = "application/x-www-form-urlencoded"
         headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36"
         headers["Accept"] = "*/*"

         return headers
      }

      fun getCookies(cep: String, dataFetcher: DataFetcher, session: Session): List<Cookie>{
         val url = "https://www.marche.com.br/deliverable_zip_codes/set_zipcode"

         val headers = getDefaultHeaders()
         headers["Referer"]="https://www.marche.com.br/home"
         headers["cookie"]= "_st_marche_ecomm_session=QW5LTkgyRXl3dFlBQWhSK29lUWl4MlREMWIxaC9VZFpxejNRY1o1NGo4M2dGNUZXT1Fxd1dHbGZFZjN3UmY0bUxOMEh4R2FkSnRoY05rQTNwVFoyZXBrT0VLZ1ErTkhpVEg1Qkh3Qk9wMTJXNG9lbjA2Z3psZGcyVTE1bVhibktJVVVHelE4ZDZVaDJObjlka2RIcmNBPT0tLUROeWJCcW1haklwanRjdzQrM1kyMGc9PQ%3D%3D--bc269ddea8e12bddc5e0654803ab3e1d47b6151b"

         val payload = "utf8=✓" +
            "&authenticity_token=uB8fn/rGYfC6CYn24bJ8GE0RYJ3cmUv2Q9cwjzldVU2z81IxUFXTeb4aQBR6WpxGZKdNFZhG5UkbXzNoaSb3lw==" +
            "&zip_code[code]=${cep}" +
            "&commit=Enviar"

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .setPayload(payload)
            .setFollowRedirects(false)
            .build()

         val response =  dataFetcher.post(session,request)

         return response.cookies
      }
   }

   override fun fetch(): Document {

      val headers = getDefaultHeaders()
      headers["Referer"] = session.originalURL

      val request = Request.RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers)
         .setCookies(this.cookies)
         .build()

      val response =  FetcherDataFetcher().get(session,request)

      return response.body.toDoc() ?: Document(session.originalURL)
   }

   private fun getToken(html:String):String{
      val document = Jsoup.parse(html)
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"head  meta[name=csrf-token]","content")
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

      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"input[name=\"product_id\"][value]", "value")

      val name = CrawlerUtils.scrapStringSimpleInfo(doc,".product-info .product-name", false)

      val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-wrapper li span")

      val offers = scrapOffers(doc)

      val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-image img", "src")

      products.add(ProductBuilder.create()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setOffers(offers)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .build())

      return products
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
