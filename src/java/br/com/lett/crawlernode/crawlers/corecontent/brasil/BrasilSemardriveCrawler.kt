package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*

/**
 * Date: 22/10/20
 *
 * @author Fellype Layunne
 *
 */

abstract class BrasilSemardriveCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Semar Driver"
   }

   init {
      config.fetcher = FetchMode.APACHE
   }

   abstract fun getZipCode(): String

   override fun handleCookiesBeforeFetch() {

      val doc = fetch()

      val token = doc.selectFirst("meta[name=csrf-token]")?.attr("content") ?: ""

      val url = "https://drive.gruposemar.com.br/current_stock"

      val headers: MutableMap<String, String> = HashMap()
      headers["accept"] = "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript"
      headers["x-csrf-token"] = token
      headers["content-type"] = "application/x-www-form-urlencoded; charset=UTF-8"

      val cookiesStr = cookies.map {
         return@map "${it.name}=${it.value};"
      }.joinToString(" ")

      headers["cookie"] = cookiesStr

      val payload = "utf8=%E2%9C%93" +
         "&_method=put" +
         "&order%5Bshipping_mode%5D=delivery" +
         "&order%5Bship_address_attributes%5D%5Btemporary%5D=true" +
         "&order%5Bship_address_attributes%5D%5Bzipcode%5D=${getZipCode()}" +
         "&button="

      val response = FetcherDataFetcher().post(
         session, Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .build()
      )

      cookies = response?.cookies ?: listOf()
   }

   override fun fetch(): Document {

      val token = cookies.firstOrNull {
         it.name == "guest_token"
      }?.value

      val headers: MutableMap<String, String> = HashMap()

      if (token != null) {
         val cookiesStr = cookies.map {
            return@map "${it.name}=${it.value};"
         }.joinToString(" ")

         headers["cookie"] = cookiesStr
      }
      headers["authority"] = "drive.gruposemar.com.br"
      headers["accept"] = "text/html, application/xhtml+xml"
      headers["user-agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36"

      val request = Request.RequestBuilder.create()
         .setUrl(session.originalURL)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      cookies = response.cookies

      return Jsoup.parse(response?.body ?: "")
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = doc.selectFirst(".product-details .product-name")?.ownText()

      val internalId = doc.selectFirst(".product-details .clearfix input[name=variant_id]")?.attr("value")

      val primaryImage = doc.selectFirst(".product-details .big-image img")?.attr("data-zoom-image")

      val offers = scrapOffers(doc)

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

      val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".checkout-container .principal span", null, false, ',', session)

      price ?: return offers

      val bankSlip = price.toBankSlip()

      val creditCards = listOf(
         Card.AMEX,
         Card.AURA,
         Card.CABAL,
         Card.ELO,
         Card.HIPER,
         Card.HIPERCARD,
         Card.MASTERCARD,
         Card.SOROCRED,
         Card.VISA
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
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .build()
      )

      return offers
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".product-details .product-name") != null
   }
}
