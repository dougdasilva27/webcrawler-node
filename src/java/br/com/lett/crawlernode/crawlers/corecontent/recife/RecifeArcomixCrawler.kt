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
                    val price = model.optFloat("mny_vlr_promo_tabela_preco")
                    val prices = scrapPrices(model, price)

                    val categories = mutableListOf<String>()
                    categories.opt(productJson.optString("str_categoria", null))
                    categories.opt(productJson.optString("str_subcategoria", null))
                    categories.opt(productJson.optString("str_tricategoria", null))


                    val name =
                        "${productJson.optString("str_nom_produto", "")} ${model
                            .optString("str_nom_produto_modelo", "")}".trim()

                    val imagesJson = JSONUtils.stringToJson(json.optJSONArray("Imagens")?.opt(0)?.toString()) ?: JSONObject()
                    products += ProductBuilder.create()
                        .setUrl(session.originalURL)
                        .setInternalId(productJson.opt("id_produto")?.toString())
                        .setInternalPid(model.opt("id_produto_modelo")?.toString())
                        .setName(name)
                        .setPrice(price)
                        .setPrices(prices)
                        .setAvailable(!model.optBoolean("bit_esgotado"))
                        .setCategories(categories)
                        .setPrimaryImage("${imagesJson.optString("str_img_path")}-g.jpg")
                        .setStock(productJson.optInt("int_qtd_estoque_produto"))
                        .setEans(mutableListOf<String>().also { list ->
                            list.opt(productJson.optString("str_cod_barras_produto"))
                        })
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
}

fun <T> MutableList<T>.opt(element: T?) {
    element?.let(this::add)
}