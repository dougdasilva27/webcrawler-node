package br.com.lett.crawlernode.crawlers.corecontent.portoalegre

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.corecontent.recife.opt
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toJson
import models.Offer.OfferBuilder
import models.Offers
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
    return dataFetcher.get(
      session, RequestBuilder.create()
        .setUrl("https://delivery.atacadotreichel.com.br/api/produto?id=${id!!}")
        .build()
    ).body?.toJson()!!
  }

  override fun extractInformation(json: JSONObject): MutableList<Product> {
    val products = mutableListOf<Product>()
    val images = mutableListOf<String>()

    for (imgJson in json.optJSONArray("str_img_path")) {
      if (imgJson is JSONObject) {

        images.opt(imgJson.optString("str_img_path"))
      }
    }

    for (elem in json.optJSONArray("Produtos")) {
      if (elem is JSONObject) {

        val internalId = json.optString("id_produto")
        val offers = scrapOffers(elem)
        val name = elem.optString("str_nom_produto")
        val eans = listOf(elem.optString("str_cod_barras_produto"))
        val stock = elem.optInt("int_qtd_estoque_produto")
        val description = elem.optString("str_meta_description_ecom_produto")

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
          .build()
      }
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
      .setPriceFrom(priceFrom)
      .build()

    val offer = OfferBuilder.create()
      .setPricing(pricing)
      .setIsMainRetailer(true)
      .setUseSlugNameAsInternalSellerId(true)
      .build()
    offers.add(offer)

    return offers
  }
}
