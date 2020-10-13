package br.com.lett.crawlernode.crawlers.corecontent.recife

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.RatingsReviews
import models.prices.Prices
import org.json.JSONObject

class RecifeArcomixCrawler(session: Session?) : Crawler(session) {

    val homePage = "https://arcomix.com.br/"

    override fun fetch(): Any {
        val skuId =
            """(?<word>produto[/].*/)""".toRegex().find(session.originalURL)?.value
                ?.trim()?.split("/")?.get(1)
        val request = RequestBuilder().setUrl("https://arcomix.com.br/api/produto?id=$skuId").build()
        return JSONUtils.stringToJson(dataFetcher.get(session, request).body)
    }

    override fun shouldVisit(): Boolean {
        val href = session.originalURL.toLowerCase()
        return !FILTERS.matcher(href)
            .matches() && href.startsWith(homePage)
    }

    override fun extractInformation(json: JSONObject?): MutableList<Product> {
        val products = mutableListOf<Product>()
        val modelos = json?.optJSONArray("Modelos")
        val productJson = JSONUtils.stringToJson(json?.optJSONArray("Produtos")?.opt(0)?.toString())?: JSONObject()
        if (modelos != null) {
            for (model in modelos) {
                if (model is JSONObject) {
                    val internalId = productJson.opt("id_produto").toString()
                    val price = model.optFloat("mny_vlr_promo_tabela_preco")
                    val prices = scrapPrices(model, price)

                    val categories = mutableListOf<String>()
                    categories addNonNull productJson.optString("str_categoria", null)
                    categories addNonNull productJson.optString("str_subcategoria", null)
                    categories addNonNull productJson.optString("str_tricategoria", null)


                    val name =
                        "${productJson.optString("str_nom_produto", "")} ${model
                            .optString("str_nom_produto_modelo", "")}".trim()

                   val arrayOfImages = CrawlerUtils.scrapImagesListFromJSONArray(json.optJSONArray("Imagens"), "str_img_path", null, "https", "arcomixstr.blob.core.windows.net", session)
                   val primaryImage = arrayOfImages.removeAt(0)

                   var secondaryImages: MutableList<String> = mutableListOf<String>()

                     for (secondary in arrayOfImages){
                         val s = "$secondary-g.jpg"
                        secondaryImages.add(s)
                     }

                   val ratingsReviews = scrapRatingReviews(internalId)



                    products += ProductBuilder.create()
                        .setUrl(session.originalURL)
                        .setInternalId(internalId)
                        .setInternalPid(model.opt("id_produto_modelo")?.toString())
                        .setName(name)
                        .setPrice(price)
                        .setPrices(prices)
                        .setAvailable(!model.optBoolean("bit_esgotado"))
                        .setCategories(categories)
                        .setPrimaryImage("${primaryImage}-g.jpg")
                       .setSecondaryImages(secondaryImages)
                        .setStock(productJson.optInt("int_qtd_estoque_produto"))
                        .setEans(mutableListOf<String>().also { list ->
                           list.addNonNull(productJson.optString("str_cod_barras_produto"))
                        })
                       .setRatingReviews(ratingsReviews)
                        .build()
                }
            }
        }
        if (products.isEmpty()) {
            Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
        }

        return products
    }

    private fun scrapPrices(model: JSONObject, priceHighlight: Float?): Prices {
        val prices = Prices()
        if (priceHighlight != null) {
            prices.apply {
                val price = model.optDouble("mny_vlr_produto_tabela_preco")
                bankTicketPrice = priceHighlight.toBigDecimal().setScale(2).toDouble()
                priceFrom = if (!priceHighlight.equals(price.toBigDecimal().setScale(2).toFloat())) price else null
                val installmentPriceMap = mutableMapOf<Int, Float>()
                installmentPriceMap[1] = priceHighlight
                insertCardInstallment(Card.VISA.toString(), installmentPriceMap)
                insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap)
                insertCardInstallment(Card.DINERS.toString(), installmentPriceMap)
                insertCardInstallment(Card.ELO.toString(), installmentPriceMap)
            }
        }
        return prices
    }

   private fun scrapRatingReviews(internalId: String) : RatingsReviews {

      val ratingsReviews = RatingsReviews()
      val reviews: JSONObject

      val response = dataFetcher.get(
         session, RequestBuilder.create()
         .setUrl("https://arcomix.com.br/api/produto/GetAvaliacaoProduto?id=$internalId")
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
