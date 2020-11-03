package br.com.lett.crawlernode.crawlers.corecontent.recife

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.addNonNull
import models.AdvancedRatingReview
import models.RatingsReviews
import models.prices.Prices
import org.json.JSONArray
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

                   val description = productJson.optString("str_meta_description_ecom_produto")

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
                        .setDescription(description)
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

   private fun fetchRating(internalId: String): JSONObject {
      val apiUrl = "https://arcomix.com.br/api/produto/GetAvaliacoesClienteProduto?id=$internalId&pag=undefined"

      val request: Request = RequestBuilder.create().setUrl(apiUrl).build()

      val response = dataFetcher[session, request].body

      return JSONUtils.stringToJson(response)
   }

   private fun scrapRatingReviews(internalId: String): RatingsReviews? {
      val jsonResponse = fetchRating(internalId)

      val avaliacoes = jsonResponse.optJSONArray("AvaliacaoCliente")

      val ratingsReviews = RatingsReviews()
      var totalRating = 0
      var totalValueReviews = 0

      if (avaliacoes != null && !avaliacoes.isEmpty) {
         totalRating = avaliacoes.length()
         ratingsReviews.setTotalRating(totalRating)
         ratingsReviews.totalWrittenReviews = totalRating

         for (e in avaliacoes) {
            totalValueReviews += (e as JSONObject).optInt("int_nota_review")
         }

         ratingsReviews.averageOverallRating = totalValueReviews.toDouble() / totalRating
         ratingsReviews.advancedRatingReview = scrapAdvancedRatingReviews(avaliacoes)
      } else {
         ratingsReviews.setTotalRating(totalRating)
         ratingsReviews.totalWrittenReviews = totalRating
         ratingsReviews.averageOverallRating = 0.0
      }
      return ratingsReviews
   }

   private fun scrapAdvancedRatingReviews(avaliacoes: JSONArray): AdvancedRatingReview? {
      val advancedRatingReview = AdvancedRatingReview()

      var stars1 = 0
      var stars2 = 0
      var stars3 = 0
      var stars4 = 0
      var stars5 = 0

      for (e in avaliacoes) {
         val reviewValue = (e as JSONObject).optInt("int_nota_review")
         when (reviewValue) {
            1 -> stars1++
            2 -> stars2++
            3 -> stars3++
            4 -> stars4++
            5 -> stars5++
            else -> {
            }
         }
      }

      advancedRatingReview.totalStar1 = stars1
      advancedRatingReview.totalStar2 = stars2
      advancedRatingReview.totalStar3 = stars3
      advancedRatingReview.totalStar4 = stars4
      advancedRatingReview.totalStar5 = stars5
      return advancedRatingReview
   }

}
