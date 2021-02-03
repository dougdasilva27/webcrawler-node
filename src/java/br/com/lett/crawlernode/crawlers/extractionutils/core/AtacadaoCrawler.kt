package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.corecontent.belgium.BelgiumDelhaizeCrawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.apache.http.cookie.Cookie
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.*
import kotlin.collections.HashMap

/**
 * Date: 27/01/21
 *
 * @author Fellype Layunne
 *
 */
abstract class AtacadaoCrawler (session: Session) : Crawler(session){

   init {
       config.fetcher = FetchMode.FETCHER
   }
   companion object {
      const val SELLER_NAME: String = "Atacadão"

      const val HOME_PAGE: String = "https://www.atacadao.com.br"

      val cards = listOf(
         Card.AMEX,
         Card.DINERS,
         Card.DISCOVER,
         Card.ELO,
         Card.HIPERCARD,
         Card.JCB,
         Card.MASTERCARD,
         Card.VISA,
      )

      fun getCookies(dataFetcher: DataFetcher, session: Session): List<Cookie> {
         val url = HOME_PAGE

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .build()

         val response = dataFetcher.get(session, request)

         return response.cookies.filter { it.name == "csrftoken" || it.name == "sessionid" }
      }

      fun setLocation(cityId: String, dataFetcher: DataFetcher, session: Session, cookies: List<Cookie>) {
         val url = "https://www.atacadao.com.br/lazyusers/edit/"

         val token = cookies.firstOrNull { it.name == "csrftoken" }?.value ?: ""

         val headers = HashMap<String, String>()
         headers["Accept"] = "*/*"
         headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
         headers["x-csrftoken"] = token
         headers["content-type"] = "application/x-www-form-urlencoded; charset=UTF-8"
         headers["Cookie"] = CommonMethods.cookiesToString(cookies)

         val payload = "city_id=${cityId}&cnpj=false&cpf=true"

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setPayload(payload)
            .setHeaders(headers)
            .setFollowRedirects(false)
            .build()

         dataFetcher.post(session, request)
      }
   }

   abstract fun getCityId(): String

   override fun handleCookiesBeforeFetch() {

      this.cookies = getCookies(this.dataFetcher, this.session)

      setLocation(getCityId(), this.dataFetcher, this.session, this.cookies)
   }

   override fun fetch(): Document {

      val url = session.originalURL

      val headers = HashMap<String, String>()
      headers["Accept"] = "*/*"
      headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
      headers["Cookie"] = CommonMethods.cookiesToString(cookies)

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body.toDoc() ?: Document(url)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".container > h1.h1", false)

      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card div[data-pk]", "data-pk")

      val description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".card > div > div.row:not(:first-child)"))

      var categoriesArray = CrawlerUtils.scrapStringSimpleInfo(doc, ".breadcrumb-item:not(:first-child) a", false)?.split("/") ?: listOf()

      if (categoriesArray.size > 3) {
         categoriesArray = categoriesArray.subList(0, 3)
      }

      val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card .product-image-box > img[src]", "src")

      val offers = scrapOffers(doc, internalId)

         val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setDescription(description)
         .setCategories(categoriesArray)
         .setOffers(offers)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(doc: Document, internalId: String): Offers {

      val offers = Offers()

      val offersDoc = doc.select(".product-box__buttom__multipliers .js-add-product[data-pk=\"${internalId}\"]")

      if (offersDoc.isNotEmpty()) {
         for ((i, offerDoc) in offersDoc.withIndex()) {

            val spotlightPrice: Double? = CrawlerUtils.scrapDoublePriceFromHtml(offerDoc, ".dropdown-item--price", null, false, ',', session)

            spotlightPrice ?: continue

            val bankSlip = spotlightPrice.toBankSlip()

            val creditCards = cards.toCreditCards(spotlightPrice)

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
                  .setIsBuybox(true)
                  .setMainPagePosition(i)
                  .setUseSlugNameAsInternalSellerId(true)
                  .setSellerFullName(SELLER_NAME)
                  .setSales(listOf())
                  .build()
            )
         }
      } else {
         val spotlightPrice: Double? = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".box-product-information p.h1", null, false, ',', session)

         spotlightPrice ?: return offers

         val bankSlip = spotlightPrice.toBankSlip()

         val creditCards = cards.toCreditCards(spotlightPrice)

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
      }

      return offers
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.selectFirst(".container > h1.h1") != null
   }

}
