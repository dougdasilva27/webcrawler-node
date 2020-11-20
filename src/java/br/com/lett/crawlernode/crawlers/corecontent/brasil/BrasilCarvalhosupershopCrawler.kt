package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toJson
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.json.JSONObject

/**
 * Date: 11/09/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilCarvalhosupershopCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Carvalho Supershop"
   }

   private fun fetchApiToken(): String {
      val url = "https://api.carvalhosupershop.com.br/v1/auth/loja/login"

      val payload = """
       {
          "domain":"carvalhosupershop.com.br",
          "username":"loja",
          "key":"df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469"
       }
       """.trimIndent()

      val headers = mapOf(
         "content-type" to "application/json",
         "origin" to "https://www.carvalhosupershop.com.br"
      )

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build()

      val response = this.dataFetcher.post(session, request)

      if (response.lastStatusCode == 200) {
         val body = response.body.toJson()

         if (body.has("success") && body.optBoolean("success")) {
            return "Bearer ${body.getString("data")}"
         }
      }

      return ""
   }

   override fun fetch(): JSONObject {

      val apiToken = fetchApiToken()

      val internalId = session.originalURL.substringBeforeLast("/").substringAfterLast("/")

      val url = "https://api.carvalhosupershop.com.br/v1/loja/produtos/${internalId}/filial/1/centro_distribuicao/1/detalhes"

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(mapOf("authorization" to apiToken))
         .build()

      val response = dataFetcher[session, request]


      if (response.lastStatusCode == 200) {
         val body = response.body.toJson()

         if (body.has("success") && body.optBoolean("success")) {
            return body.optJSONObject("data")
         }
      }
      return JSONObject()
   }

   override fun extractInformation(doc: JSONObject): MutableList<Product> {

      if (!isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val productJson = doc.optJSONObject("produto")

      val name = productJson.optString("descricao")

      val internalId = "${productJson.optLong("produto_id")}"

      val internalPid = productJson.optString("id")

      val offersJson = productJson.optJSONObject("oferta") ?: JSONObject()

      val price = productJson.optString("preco")?.toDouble() ?: 0.0

      val available = productJson.optBoolean("disponivel")

      val stock = productJson.optString("quantidade_maxima")?.toInt()

      val description = doc.optString("informacoes")

      val primaryImage = if (productJson.has("imagem")) "https://s3.amazonaws.com/produtos.vipcommerce.com.br/250x250/${productJson.optString("imagem")}" else null

      val offers = if (available) scrapOffers(offersJson, price) else Offers()

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setStock(stock)
         .setPrimaryImage(primaryImage)
         .setDescription(description)
         .setOffers(offers)
         .setRatingReviews(null)
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(offersJson: JSONObject, price: Double): Offers {
      val offers = Offers()

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(scrapPricing(offersJson, price))
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .build()
      )

      return offers
   }

   private fun scrapPricing(offersJson: JSONObject, price: Double): Pricing {

      val spotlightPrice = offersJson.optString("preco_oferta", null)?.toDouble() ?: price

      val priceFrom = offersJson.optString("preco_antigo", null)?.toDouble()

      val bankSlip = spotlightPrice.toBankSlip()

      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.AURA, Card.DINERS, Card.HIPER, Card.AMEX).toCreditCards(spotlightPrice)

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build()
   }

   private fun isProductPage(doc: JSONObject): Boolean {
      return doc.optJSONObject("produto")!=null
   }
}
