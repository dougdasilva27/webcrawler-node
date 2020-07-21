package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.*
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

        val baseName = CrawlerUtils.scrapStringSimpleInfo(doc, ".main_product_info .product_name span", true)
        val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb ul li:not(:first-child):not(:last-child) a")
        val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value")

        for (sku: String in scrapProductVariations(doc)) {

            val jsonProduct = getProductFromApi(internalPid, sku)

            val description = jsonProduct.getString("description")

            val images = getImages(jsonProduct)

            val offers = if (jsonProduct.getBoolean("purchasable")) scrapOffers(jsonProduct, internalPid, sku) else Offers()

            val variants = mutableListOf<String>()

            jsonProduct.getJSONArray("skuAttributes").sortedBy {

                val i = it as JSONObject

                val o = i.get("priority")
                if (o is Int) {
                    o
                } else {
                    0
                }

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

    private fun scrapProductVariations(doc: Document): MutableList<String> {

        val elements = doc.select(".sku_selection .sku #js-prod-price label input")

//        val skus = mutableListOf("549982048")
        val skus = mutableListOf<String>()

        elements?.map {
            val dataRefs = it.attr("data-refs")

            val dataJson = JSONUtils.stringToJsonArray(dataRefs)

            dataJson?.map { skuData ->
                skus addNonNull (skuData as JSONObject).getString("skuId")
            }
        }

        return skus
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


            val rennerCard = CreditCard.CreditCardBuilder.create()
                    .setBrand("Cartão Renner")
                    .setIsShopCard(true)
                    .setInstallments(
                            scrapInstallments(dataOffers.getElementsByAttributeValue("data-target_content", "rennerCard").first())
                    )
                    .build()

            val otherCars = CreditCard.CreditCardBuilder.create()
                    .setBrand("Outros cartões")
                    .setIsShopCard(false)
                    .setInstallments(
                            scrapInstallments(dataOffers.getElementsByAttributeValue("data-target_content", "creditCard").first())
                    )
                    .build()

            val creditCards = CreditCards(listOf(rennerCard, otherCars))

            offers.add(
                    Offer.OfferBuilder.create()
                            .setPricing(
                                    Pricing.PricingBuilder.create()
                                            .setCreditCards(creditCards)
                                            .setSpotlightPrice(spotlightPrice)
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

    private fun getImages(doc: JSONObject): List<String> {

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
