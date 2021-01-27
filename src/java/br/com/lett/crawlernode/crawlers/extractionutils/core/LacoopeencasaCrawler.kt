package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
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

/**
 * Date: 25/01/21
 *
 * @author Fellype Layunne
 *
 */
abstract class LacoopeencasaCrawler (session: Session) : Crawler(session){

   companion object {
      const val SELLER_NAME: String = "lacoopeencasa"

      fun getHeaders(referer: String): MutableMap<String, String> {
         val headers = HashMap<String, String>()
         headers["Accept"] = "*/*"
         headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36"
         headers["Cookie"] = "_lcec_sid_inv=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZF9jYXJyaXRvIjowLCJmZWNoYSI6IjIwMjEtMDEtMjUgMTU6MjU6NDEiLCJ2ZW5jaW1pZW50byI6NDMyMDB9.KetXlwmZ7hMbEd2B13oY5B8C8yxd3e8rItIua7IbgHg; "
         headers["Referer"] = referer
         return headers
      }

      fun getCookies(dataFetcher: DataFetcher, session: Session, localId: String, referer: String): List<Cookie> {
         val url = "https://www.lacoopeencasa.coop/ws/index.php/comun/datosController/datos" +
            "?local=$localId" +
            "&modo_entrega_seleccionado=null"

         val headers = getHeaders(referer)

         headers.remove("Cookie")

         val request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .build()

         val response = dataFetcher.get(session, request)

         return response.cookies.filter {
            it.name == "_lcec_linf" && it.value.contains("id_provincia")
         }
      }
   }

   abstract fun getLocalId(): String

   private fun scrapInternalPidFromUrl(): String {
      val path = session.originalURL.substringAfterLast("/")

      return if (path.contains("?")) path.substringBeforeLast("?") else path
   }

   override fun handleCookiesBeforeFetch() {

      this.cookies = getCookies(dataFetcher, session, getLocalId(), session.originalURL)
   }

   override fun fetch(): JSONObject {

      val url =   "https://www.lacoopeencasa.coop/ws/index.php/articulo/articuloController/articulo_detalle" +
         "?cod_interno=${scrapInternalPidFromUrl()}" +
         "&simple=false" +
         "&local=${getLocalId()}"

      val headers = getHeaders(session.originalURL)

      headers["Cookie"] += this.cookies.firstOrNull { it.name == "_lcec_linf" }?.let { "${it.name}=${it.value};" } ?: ""

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      return dataFetcher.get(session, request).body.toJson()
   }

   override fun extractInformation(json: JSONObject): MutableList<Product> {

      if (!isProductPage(json)) {
         return mutableListOf()
      }

      val productData = JSONUtils.getJSONValue(json, "datos")

      val name = productData.optString("descripcion")

      val internalId = productData.optString("cod_interno")

      val description = productData.optString("desc_larga", null)

      val primaryImage = productData.optString("imagen")

      var secondaryImages = JSONUtils.getJSONArrayValue(productData, "imagenes").map { (it as String) }.toList()

      if (secondaryImages.size == 1 && secondaryImages[0] == primaryImage) {
         secondaryImages = listOf()
      }

      val offers = scrapOffers(productData)

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setOffers(offers)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(json: JSONObject): Offers {

      val offers = Offers()

      if (json.optString("disponibilidad") != "true") {
         return offers
      }

      val spotlightPrice = json.optString("precio").toDouble()

      var priceFrom: Double? = json.optString("precio_anterior").toDouble()

      if (priceFrom == spotlightPrice) {
         priceFrom = null
      }

      val bankSlip = spotlightPrice.toBankSlip()

      val creditCards = listOf(
         Card.MASTERCARD,
         Card.VISA,
         Card.DINERS,
         Card.CABAL,
         Card.NATIVA,
         Card.NARANJA,
         Card.AMEX,
      ).toCreditCards(spotlightPrice)

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(spotlightPrice)
                  .setPriceFrom(priceFrom)
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

   private fun isProductPage(json: JSONObject): Boolean {
      return !json.isEmpty && JSONUtils.getValueRecursive(json, "datos.cod_interno", String::class.java) == scrapInternalPidFromUrl()
   }

}
