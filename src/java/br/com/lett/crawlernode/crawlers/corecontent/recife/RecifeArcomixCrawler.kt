package br.com.lett.crawlernode.crawlers.corecontent.recife

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.AdvancedRatingReview
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing
import org.json.JSONArray
import org.json.JSONObject


class RecifeArcomixCrawler(session: Session?) : Crawler(session) {

   private val idArmazem = session!!.options.optString("id_armazem")

   companion object {
      const val SELLER_NAME: String = "Arco mix"
   }

   val homePage = "https://arcomix.com.br/"

   override fun fetch(): Any {

      val headers: MutableMap<String, String> = HashMap()
      headers["Cookie"] = "ls.uid_armazem=$idArmazem"

      val skuId =
         """(?<word>produto[/].*/)""".toRegex().find(session.originalURL)?.value
            ?.trim()?.split("/")?.get(1)
      val request = RequestBuilder().setUrl("https://arcomix.com.br/api/produto?id=$skuId").setHeaders(headers).build()
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
      val productJson = JSONUtils.stringToJson(json?.optJSONArray("Produtos")?.opt(0)?.toString()) ?: JSONObject()
      if (modelos != null) {
         for (model in modelos) {
            if (model is JSONObject) {
               val internalId = productJson.opt("id_produto").toString()
               val offers: Offers = scrapOffers(model)

               val categories = mutableListOf<String>()
               categories addNonNull productJson.optString("str_categoria", null)
               categories addNonNull productJson.optString("str_subcategoria", null)
               categories addNonNull productJson.optString("str_tricategoria", null)

               val description = productJson.optString("str_meta_description_ecom_produto")

               val name =
                  "${productJson.optString("str_nom_produto", "")} ${
                     model
                        .optString("str_nom_produto_modelo", "")
                  }".trim()

               val arrayOfImages = CrawlerUtils.scrapImagesListFromJSONArray(json.optJSONArray("Imagens"), "str_img_path", null, "https", "arcomixstr.blob.core.windows.net", session)
               val primaryImage = arrayOfImages.removeAt(0)
               val secondaryImages: MutableList<String> = mutableListOf()
               for (secondary in arrayOfImages) {
                  val s = "$secondary-g.jpg"
                  secondaryImages.add(s)
               }

               val ratingsReviews = scrapRatingReviews(internalId)

               products += ProductBuilder.create()
                  .setUrl(session.originalURL)
                  .setInternalId(internalId)
                  .setInternalPid(model.opt("id_produto_modelo")?.toString())
                  .setName(name)
                  .setOffers(offers)
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

   private fun scrapOffers(model: JSONObject): Offers {

      val offers = Offers()
      val sale = JSONUtils.getDoubleValueFromJSON(model, "mny_perc_desconto", true)
      val sales: MutableList<String> = ArrayList()
      if (sale != null){
         sales.add(sale.toString())

      }

      val notAvailable = model.optBoolean("bit_esgotado")

      if (notAvailable) {
         return offers
      }

      val spotlightPrice = JSONUtils.getDoubleValueFromJSON(model, "mny_vlr_promo_tabela_preco", true)
      var priceFrom = JSONUtils.getDoubleValueFromJSON(model, "mny_vlr_produto_tabela_preco", true)

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

   private fun scrapSales(pricing: Pricing): List<String>? {
      val sales: MutableList<String> = ArrayList()
      val saleDiscount = CrawlerUtils.calculateSales(pricing)
      if (saleDiscount != null) {
         sales.add(saleDiscount)
      }
      return sales
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
