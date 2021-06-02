package br.com.lett.crawlernode.crawlers.corecontent.portoalegre

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing
import org.json.JSONObject

class PortoalegreTreichelCrawler(session: Session) : Crawler(session) {

  override fun fetch(): Any {
    val tokens = session.originalURL.split("/")

    var id: String? = null
    tokens.forEachIndexed { index, s ->
      if (s == "produto") {
        id = tokens[index + 1]
      }
    }

    val response = dataFetcher.get(
      session, RequestBuilder.create()
        .setUrl("https://delivery.atacadotreichel.com.br/api/produto?id=${id}")
        .build()
    ).body

     if(response != null){
        return JSONUtils.stringToJson(response)
     }
     return response
  }

  override fun extractInformation(json: JSONObject): MutableList<Product> {
     val products = mutableListOf<Product>()
     val images = mutableListOf<String>()

     // Is a product page
     if(json.has("id_produto")){
        for (imgJson in json.optJSONArray("Imagens")) {
           if (imgJson is JSONObject) {

              images += "${imgJson.optString("str_img_path")}-g.jpg"
           }
        }

        for (elem in json.optJSONArray("Produtos")) {
           if (elem is JSONObject) {

              val internalId = json.optString("id_produto")
              val offers = scrapOffers(elem)
              val name = elem.optString("str_nom_produto")
              val eans = listOf(elem.optString("str_cod_barras_produto"))
              val stock = elem.optInt("int_qtd_estoque_produto")
              val description = elem.optString("str_html_descricao_produto")
              val ratingsReviews = scrapRatingReviews(internalId)

              products += ProductBuilder.create()
                 .setUrl(session.originalURL)
                 .setInternalId(internalId)
                 .setName(name)
                 .setStock(stock)
                 .setPrimaryImage(images.removeAt(0))
                 .setSecondaryImages(images)
                 .setDescription(description)
                 .setEans(eans)
                 .setOffers(offers)
                 .setRatingReviews(ratingsReviews)
                 .build()
           }
        }
     } else{
        Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
     }

    return products
  }

  private fun scrapOffers(elem: JSONObject): Offers {
    val offers = Offers()
    val price = elem.optDouble("mny_vlr_promo_tabela_preco")
    val priceFrom = elem.optDouble("mny_vlr_produto_tabela_preco")

    val pricing = Pricing.PricingBuilder.create()
      .setBankSlip(price.toBankSlip())
      .setSpotlightPrice(price)
      .setCreditCards(setOf(VISA, MASTERCARD, HIPERCARD, HIPER, DINERS, JCB).toCreditCards(price))
      .setPriceFrom(if (priceFrom != price) priceFrom else null)
      .build()

    val offer = OfferBuilder.create()
      .setPricing(pricing)
      .setIsMainRetailer(true)
      .setSellerFullName("Treichel")
      .setUseSlugNameAsInternalSellerId(true)
      .setIsBuybox(false)
      .build()
    offers.add(offer)

    return offers
  }

   private fun scrapRatingReviews(internalId: String) : RatingsReviews{

      val ratingsReviews = RatingsReviews()
      val reviews: JSONObject

      val response = dataFetcher.get(
         session, RequestBuilder.create()
         .setUrl("https://delivery.atacadotreichel.com.br/api/produto/GetAvaliacaoProduto?id=$internalId")
         .build()
      ).body

      if(response != null){
         reviews = JSONUtils.stringToJson(response)
         ratingsReviews.setTotalRating(reviews.optInt("intQtdAvaliacao"))
         ratingsReviews.totalWrittenReviews = reviews.optInt("intQtdAvaliacao")
         ratingsReviews.averageOverallRating = reviews.optDouble("intNotaAvaliacao")
      }
      return ratingsReviews
   }
}
