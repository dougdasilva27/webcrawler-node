package br.com.lett.crawlernode.crawlers.corecontent.portoalegre

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.toJson
import models.Offer.OfferBuilder
import models.Offers
import org.json.JSONObject

class TreichelCrawler(session: Session) : Crawler(session) {

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
    val internalPid = json.optString("id_produto")

    for (elem in json.optJSONArray("Produtos")) {
      if (elem is JSONObject) {
        val offers = scrapOffers(elem)
      }
    }
    /*
      .setUrl(session.getOriginalURL())
      .setInternalId(internalId)
      .setInternalPid(internalPid)
      .setName(variationName != null ? name + " " + variationName : name)
      .setCategory1(categories.getCategory(0))
      .setCategory2(categories.getCategory(1))
      .setCategory3(categories.getCategory(2))
      .setPrimaryImage(primaryImage)
      .setSecondaryImages(secondaryImages)
      .setDescription(description)
      .setEans(eans)
      .setOffers(offers)
      .build();
     */
    return super.extractInformation(json)
  }

  private fun scrapOffers(elem: JSONObject): Offers {
    val offers = Offers()
    val price = elem.optDouble("mny_vlr_promo_tabela_preco")
    val priceFrom = elem.optDouble("mny_vlr_produto_tabela_preco")
    OfferBuilder.create()
    return offers
  }
}
