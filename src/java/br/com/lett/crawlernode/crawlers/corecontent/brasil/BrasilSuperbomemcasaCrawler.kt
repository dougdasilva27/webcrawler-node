package br.com.lett.crawlernode.crawlers.corecontent.brasil

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
import org.json.JSONArray
import org.json.JSONObject

/**
 * Date: 21/12/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilSuperbomemcasaCrawler (session: Session) : Crawler(session){

   companion object {
      const val SELLER_NAME: String = "Superbomemcasa"

      fun scrapAttributes(jsonArray: JSONArray, key: String): String? {
         for (att in jsonArray) {
            if (att is JSONObject) {
               if (att.optString("attribute_code") == key) {
                  return att.optString("value")
               }
            }
         }
         return null
      }
   }

   private fun scrapInternalPidFromUrl(): String {

      return session.originalURL.substringAfterLast(".").substringBeforeLast("p")
   }

   override fun fetch(): JSONObject {

      val url =   "https://sb.superbomemcasa.com.br/rest/default/V1/products/" + scrapInternalPidFromUrl()

      val headers = HashMap<String, String>()

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build()

      return dataFetcher.get(session, request).body.toJson()
   }

   override fun extractInformation(json: JSONObject): MutableList<Product> {

      if (!isProductPage()) {
         return mutableListOf()
      }

      val name = json.optString("name")
      val internalId = json.optString("id")
      val internalPid = json.optString("sku")

      val jsonAtt = JSONUtils.getJSONArrayValue(json, "custom_attributes")

      val description = scrapAttributes(jsonAtt, "description")

      var primaryImage = scrapAttributes(jsonAtt, "swatch_image")

      if (primaryImage != null) {
         primaryImage = "https://superbom.s3-sa-east-1.amazonaws.com/catalog/product${primaryImage}"
      }

      val offers = scrapOffers(json, jsonAtt)

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setDescription(description)
         .setOffers(offers)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(json: JSONObject, jsonAtt: JSONArray): Offers {

      val offers = Offers()

      if (json.optInt("status") != 1) {
         return offers
      }

      val price = json.optDouble("price")

      val specialPrice = scrapAttributes(jsonAtt, "special_price")?.toDouble()

      val spotlightPrice = specialPrice ?: price

      val priceFrom = if (specialPrice == null) null else price

      val bankSlip = spotlightPrice.toBankSlip()

      val creditCards = listOf(
         Card.MASTERCARD,
         Card.VISA,
         Card.HIPERCARD,
         Card.ELO,
         Card.DINERS,
         Card.AMEX,
         Card.SOROCRED,
         Card.CABAL,
         Card.JCB
      ).toCreditCards(price)

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

   private fun isProductPage(): Boolean {
      return scrapInternalPidFromUrl().isNotEmpty()
   }

}
