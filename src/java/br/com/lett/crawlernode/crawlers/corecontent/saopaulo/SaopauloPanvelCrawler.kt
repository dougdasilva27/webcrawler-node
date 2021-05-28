package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.round
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toJson
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.time.LocalDate

class SaopauloPanvelCrawler(session: Session) : Crawler(session) {

   init {
      config.fetcher = FetchMode.FETCHER
      cookies.add(BasicClientCookie("stc112189", LocalDate.now().toEpochDay().toString()))
   }

   override fun fetch(): Any? {
      val request = Request.RequestBuilder.create().setCookies(cookies).setUrl(session.originalURL).build()
      var doc: Document? = null

      for (i in 1..3) {
         val response = dataFetcher[session, request]
         if (checkResponse(response)) {
            doc = Jsoup.parse(response.body)
            break
         }
      }
      return doc
   }

   override fun handleCookiesBeforeFetch() {
      val request = Request.RequestBuilder.create().setCookies(cookies).setUrl("https://www.panvel.com/panvel/main.do").build()
      cookies.addAll(dataFetcher[session, request].cookies)
   }

   private fun checkResponse(response: Response): Boolean {
      val statusCode = response.lastStatusCode.toString()
      return statusCode[0] == '2' || statusCode[0] == '3' || statusCode == "404"
   }

   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products: MutableList<Product> = ArrayList()
      if (""".*p-\d*""".toRegex().matches(session.originalURL)) {
         val internalId = URL(session.originalURL).path.substringAfterLast("-")

         val json = doc.selectFirst("#serverApp-state").data()
            .replace("&q;", "\"")
            .replace("&s;", "'")
            .replace("&g;", ">")
            .replace("&l;", "<")
            .toJson().optJSONObject("api/v1/item/$internalId")

         val categories = json.optJSONArray("categories").map { (it as JSONObject).optString("description") }
         val jsonImages = json.optJSONArray("images").sortedBy { (it as JSONObject).optInt("number") }.toMutableList()
         val primaryImage = (jsonImages.removeFirst() as JSONObject).optString("url")
         val secondaryImages = jsonImages.map { (it as JSONObject).optString("url") }
         val name = json.optString("name")
         val offers = scrapOffers(json)

         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .build()
         products.add(product)
      }

      return products
   }

   private fun scrapOffers(json: JSONObject): Offers {
      var price = json.optDouble("originalPrice")
      var priceFrom: Double? = null
      val discount = (json.optQuery("/discount/percentage") as Int?)
      if (discount != 0) {
         price *= (1 - (json.optQuery("/discount/percentage") as Int).toDouble() / 100)
         priceFrom = json.optDouble("originalPrice")
      }
      val bankSlip = price.toBankSlip()
      val creditCards = listOf(Card.HIPERCARD, Card.VISA, Card.MASTERCARD, Card.AMEX, Card.DINERS).toCreditCards(price)

      val offer = Offer.OfferBuilder.create()
         .setPricing(
            Pricing.PricingBuilder.create()
               .setSpotlightPrice(price.round())
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .setBankSlip(bankSlip)
               .build()
         )
         .setUseSlugNameAsInternalSellerId(true)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSellerFullName("Panvel")
         .build()

      return Offers(listOf(offer))
   }
}
