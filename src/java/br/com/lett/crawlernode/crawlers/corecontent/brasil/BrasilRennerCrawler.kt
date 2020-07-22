package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricingException
import models.Offer
import models.Offers
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment
import models.pricing.Installments
import models.pricing.Pricing
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


/**
 * Date: 14/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilRennerCrawler(session: Session) : Crawler(session) {

    companion object {
        const val SELLER_NAME: String = "Renner"
    }

    override fun extractInformation(doc: Document): MutableList<Product> {
        super.extractInformation(doc)

        if (!isProductPage(doc)) {
            return mutableListOf()
        }

        val products = mutableListOf<Product>()

        val baseName = CrawlerUtils.scrapStringSimpleInfo(doc, ".main_product .product_name span", true)
        val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb ul li:not(:first-child):not(:last-child) a")
        val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value")

        val skus = scrapProductVariations(doc)

        skus.map { sku ->

            val jsonProduct = getProductFromApi(internalPid, sku)

            val description = jsonProduct.getString("description")

            val images = scrapImages(jsonProduct)

            val offers = if (jsonProduct.getBoolean("purchasable")) scrapOffers(jsonProduct, internalPid, sku) else Offers()

            val variants = mutableListOf<String>()

            jsonProduct.getJSONArray("skuAttributes")
                    .filter {
                        val i = it as JSONObject
                        val c = i.optString("code")
                        val m = i.optJSONObject("mediaSet")

                        return@filter (!(c.isNotEmpty() && m == null))
                    }
                    .sortedBy {
                        (it as JSONObject).optString("code") ?: ""
                    }.map {
                        variants addNonNull (it as JSONObject).getString("name")?.trim()?.toUpperCase()
                    }

            val name = "${baseName.toUpperCase()} ${variants.joinToString(separator = " ")}"

            val product = ProductBuilder()
                    .setUrl(session.originalURL)
                    .setInternalId(sku)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategories(categories)
                    .setPrimaryImage(images[0])
                    .setSecondaryImages(images.subList(1, images.size))
                    .setDescription(description)
                    .setOffers(offers)
                    .setRatingReviews(null)
                    .build()

            products addNonNull product
        }
        return products
    }

    private fun scrapProductVariations(doc: Document): List<String> {

        val ids = mutableListOf<String>()

        val elementsWithColors = doc.select("#js-buy-form input[data-refs]")

        if (elementsWithColors.isNotEmpty()) {
            for (e in elementsWithColors) {
                val skus: JSONArray = JSONUtils.stringToJsonArray(e.attr("data-refs"))
                for (o in skus) {
                    val skuObj = (o as JSONObject).optString("skuId")
                    if (skuObj.isNotEmpty()) {
                        ids += skuObj
                    }
                }
            }
        } else {
            val options = doc.select("#js-buy-form input[value]")
            for (e in options) {
                ids addNonNull e.`val`()
            }
        }

        return ids
    }

    private fun scrapInstallments(doc: Element): Installments {
        val installments = Installments()

        doc.select("table tbody tr").map {
            val text = it.text().replace("/^(<strong>|</strong>)\$/", "")
            val pair = CrawlerUtils.crawlSimpleInstallmentFromString(text, "de", "s/ juros", true)

            val installment = Installment.InstallmentBuilder
                    .create()
                    .setInstallmentNumber(pair.first)
                    .setInstallmentPrice(pair.second.round())
                    .setFinalPrice((pair.first * pair.second).round())
                    .build()

            installments.add(installment)
        }
        return installments
    }

    private fun scrapOffers(doc: JSONObject, productId: String, skuId: String): Offers {

        val offers = Offers()

        val dataOffers = getInstallmentsFromApi(productId, skuId)

        val priceText = doc.getString("listPriceFormatted")
        val spotlightText = doc.getString("salePriceFormatted")

        var priceFrom = MathUtils.parseDoubleWithComma(priceText)
        val spotlightPrice = if (doc.getDouble("percentDiscount") > 0) {
            MathUtils.parseDoubleWithComma(spotlightText)
        } else {
            priceFrom
        }

        spotlightPrice?.let {
            if (spotlightPrice == priceFrom) {
                priceFrom = null
            }

            val sales = mutableListOf<String>()

            sales addNonNull doc.getDouble("percentDiscount").toString()


            val rennerCard = CreditCardBuilder()
                    .setBrand("Cartão Renner")
                    .setIsShopCard(true)
                    .setInstallments(
                            scrapInstallments(dataOffers.getElementsByAttributeValue("data-target_content", "rennerCard").first())
                    )
                    .build()

            val otherCars =
                    listOf(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO,
                            Card.HIPERCARD).map { card: Card ->
                        try {
                            return@map CreditCardBuilder.create()
                                    .setBrand(card.toString())
                                    .setIsShopCard(false)
                                    .setInstallments(
                                            scrapInstallments(dataOffers.getElementsByAttributeValue("data-target_content", "creditCard").first())
                                    )
                                    .build()
                        } catch (e: MalformedPricingException) {
                            throw RuntimeException(e)
                        }
                    }

            val creditCards = CreditCards(otherCars)

            creditCards.add(rennerCard)

            offers.add(
                    Offer.OfferBuilder.create()
                            .setPricing(
                                    Pricing.PricingBuilder.create()
                                            .setCreditCards(creditCards)
                                            .setSpotlightPrice(spotlightPrice)
                                            .setBankSlip(spotlightPrice.toBankSlip())
                                            .setPriceFrom(priceFrom)
                                            .build()
                            )
                            .setSales(sales)
                            .setIsMainRetailer(true)
                            .setIsBuybox(false)
                            .setUseSlugNameAsInternalSellerId(true)
                            .setSellerFullName(SELLER_NAME)
                            .build()
            )
        }

        return offers
    }

    private fun scrapImages(doc: JSONObject): List<String> {

        return doc.getJSONArray("mediaSets").map { "http:${(it as JSONObject).getString("mediumImageUrl")}" }
    }

    private fun getProductFromApi(productId: String, skuId: String): JSONObject {
        val url = "https://www.lojasrenner.com.br/rest/model/lrsa/api/CatalogActor/refreshProductPage?skuId=$skuId&productId=$productId"

        val request: Request = Request.RequestBuilder.create().setUrl(url)
                .mustSendContentEncoding(false)
                .build()
        val response: Response = dataFetcher.get(session, request)

        return CrawlerUtils.stringToJson(response.body)
    }

    private fun getInstallmentsFromApi(productId: String, skuId: String): Document {
        val url = "https://www.lojasrenner.com.br/store/renner/br/components/ajax/modalCard.jsp?skuId=$skuId&productId=$productId"

        val request: Request = Request.RequestBuilder.create().setUrl(url)
                .mustSendContentEncoding(false)
                .build()
        val response: Response = dataFetcher.get(session, request)

        return Jsoup.parse(response.body)
    }

    private fun isProductPage(document: Document): Boolean {
        return document.selectFirst(".product_name") != null
    }
}
