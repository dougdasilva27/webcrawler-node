package br.com.lett.crawlernode.crawlers.corecontent.recife

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.Logging
import models.prices.Prices
import org.json.JSONObject

class RecifeArcomixCrawler(session: Session?) : Crawler(session) {

    val homePage = "https://arcomix.com.br/"

    override fun fetch(): Any {
        val skuId =
            """(produto/\d+)""".toRegex().find(session.originalURL)?.value
                ?.replace("[^0-9]".toRegex(), "")?.trim()
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
        val productJson = JSONUtils.stringToJson(json?.optJSONArray("Produtos")?.opt(0)?.toString())
        if (modelos != null) {
            for (model in modelos) {
                if (model is JSONObject) {
                    val price = model.optFloat("mny_vlr_promo_tabela_preco")
                    val prices = scrapPrices(model, price)

                    val categories = listOf(
                        productJson.optString("str_categoria"),
                        productJson.optString("str_subcategoria"),
                        productJson.optString("str_tricategoria")
                    )

                    val name =
                        "${productJson.optString("str_nom_produto")} ${model.optString("str_nom_produto_modelo")}"

                    val imagesJson = JSONUtils.stringToJson(json.optJSONArray("Imagens")?.get(0).toString())
                    products += ProductBuilder.create()
                        .setUrl(session.originalURL)
                        .setInternalId(productJson.optInt("id_produto").toString())
                        .setInternalPid(model.optInt("id_produto_modelo").toString())
                        .setName(name)
                        .setPrice(price)
                        .setPrices(prices)
                        .setAvailable(!model.optBoolean("bit_esgotado"))
                        .setCategories(categories)
                        .setPrimaryImage(imagesJson.optString("str_img_path"))
                        .setStock(productJson.optInt("int_qtd_estoque_produto"))
                        .setEans(listOf(productJson.optString("str_cod_barras_produto")))
                        .build()
                }
            }
        }
        if (products.isEmpty()) {
            Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
        }

        return products
    }

    private fun scrapPrices(model: JSONObject, priceHighlight: Float) = Prices().apply {
        val price = model.optDouble("mny_vlr_produto_tabela_preco")
        bankTicketPrice = priceHighlight.toBigDecimal().setScale(2).toDouble()
        priceFrom = if (!priceHighlight.equals(price.toFloat())) price else null
        val installmentPriceMap = mutableMapOf<Int, Float>()
        installmentPriceMap[1] = priceHighlight
        insertCardInstallment(Card.VISA.toString(), installmentPriceMap)
        insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap)
        insertCardInstallment(Card.DINERS.toString(), installmentPriceMap)
        insertCardInstallment(Card.ELO.toString(), installmentPriceMap)
    }
}
