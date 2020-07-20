package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.fetcher.models.Response
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.*


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

        val jsonInfo = crawlerJsonInfo(doc)

        print(jsonInfo)
        if (!isProductPage(doc)) {
            return mutableListOf()
        }

        val products = mutableListOf<Product>()


//        val name = doc.selectFirst(".product_name")?.selectFirst("span")?.text()
        val name = jsonInfo.getString("name")

//        val productHtmlId = doc.selectFirst("#js-product-form")?.getElementsByAttributeValue("name", "product")?.attr("value") ?: ""
        val internalPid = jsonInfo.getString("mpn")

//        val productId = doc.selectFirst("#js-product-form")?.getElementsByAttributeValue("name", "sku")?.attr("value") ?: ""
        val productId = jsonInfo.getString("sku")


        //[{"id":"TAMG","skuId":"549982064"},{"id":"TAMGG","skuId":"549982072"},{"id":"TAMP","skuId":"549982048"},{"id":"TAMM","skuId":"549982056"}]
        //[{"id":"TAMG","skuId":"549977708"},{"id":"TAMGG","skuId":"549977716"},{"id":"TAMP","skuId":"549977687"},{"id":"TAMM","skuId":"549977695"}]
        print("product :$internalPid")
        //val productId = getProductIdFromApi(productHtmlId) //"5921935"
        //val productId = productNameHtml.selectFirst("small")?.text()?.split(": ")?.get(1) ?: ""

        print("unique: $productId")


        val images = getImages(doc)

        val description = doc.selectFirst(".desc").html()

        val categories = crawlerCategories(doc)

        val price = jsonInfo.getJSONObject("offers")?.getString("price")


        val offers = scrapOffers(doc)

        val product = ProductBuilder()
                .setUrl(session.originalURL)
                .setInternalId(productId)
                .setInternalPid(internalPid)
                .setName(name) //falta cor
                .setCategories(categories)
                .setPrimaryImage(images[0])
                .setSecondaryImages(images.subList(1, images.size))
                .setDescription(description)

                .setStock(1)
                .setEans(mutableListOf())
                .setOffers(offers)
                .setRatingReviews(null)
                .build()

        print(product.toString())

        products += product

        return products
    }

    private fun scrapOffers(doc: Document): Offers {
        val jsonInfo = crawlerJsonInfo(doc)

        val offers = Offers()

        val priceDiv = doc.selectFirst("#js-div-buy .product_price .wrap")

        val priceFromText = priceDiv?.selectFirst(".old_price")?.text()
        val spotlightPriceText = priceDiv?.selectFirst(".best_price")?.text()

        var priceFrom = MathUtils.parseDoubleWithComma(priceFromText)
        val spotlightPrice = MathUtils.parseDoubleWithComma(spotlightPriceText)

        spotlightPrice?.let {
            if (spotlightPrice == priceFrom) {
                priceFrom = null
            }

            val sales = mutableListOf<String>()

            sales addNonNull doc.selectFirst("#percentOff")?.text()

            val creditCards = setOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX).toCreditCards(spotlightPrice)

            offers.add(
                    Offer.OfferBuilder.create()
                            .setPricing(
                                    Pricing.PricingBuilder.create()
                                            .setCreditCards(creditCards)
                                            .setBankSlip(spotlightPrice.toBankSlip())
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

    private fun crawlerJsonInfo(doc: Document): JSONObject {
        return CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]",null, null, false, true)
    }

    private fun getImages(doc: Document): List<String> {
        return doc.selectFirst(".image_area")?.children()?.map {

            val imageLink = it.selectFirst("img")?.attr("data-medium-image") ?: ""

            if (imageLink.isNotEmpty()) "http:$imageLink" else ""
        }?.filter { it.isNotEmpty() } ?: mutableListOf()
    }

    fun getProduFromApi(productHtmlId: String): Any {
        val headers: MutableMap<String, String> = HashMap()

        headers["authority"] = "vfr-v3-production.sizebay.technology"
        headers["dnt"] = "1"
        headers["x-requested-with"] = "XMLHttpRequest"
        headers["accept"] = "*/*"
        headers["origin"] = "https://www.lojasrenner.com.br"
        headers["sec-fetch-site"] = "cross-site"
        headers["sec-fetch-mode"] = "cors"
        headers["sec-fetch-dest"] = "empty"

        val url = "https://vfr-v3-production.sizebay.technology/plugin/product/$productHtmlId"

        val request: Request = Request.RequestBuilder.create().setUrl(url)
                .setHeaders(headers)
                .mustSendContentEncoding(false)
                .build()
        val response: Response = dataFetcher.post(session, request)

        return CrawlerUtils.stringToJson(response.body)
    }

    private fun crawlerCategories(doc: Document): List<String> {
        val categories = mutableListOf<String>()

        val elements = doc.selectFirst(".breadcrumb div ul").select("li")

        for (i: Int in 1 until elements.size ) {
            categories.add(elements[i].text())
        }

        return categories
    }

    private fun getProductIdFromApi(productHtmlId: String): String {

        val headers = HashMap<String, String>()

        headers["authority"] = "vfr-v3-production.sizebay.technology"
        headers["dnt"] = "1"
        headers["x-requested-with"] = "XMLHttpRequest"
        headers["accept"] = "*/*"
        headers["origin"] = "https://www.lojasrenner.com.br"
        headers["sec-fetch-site"] = "cross-site"
        headers["sec-fetch-mode"] = "cors"
        headers["sec-fetch-dest"] = "empty"

        val url = "https://vfr-v3-production.sizebay.technology/plugin/my-product-id?permalink=https://www.lojasrenner.com.br/p/-/A-${productHtmlId}-br.lr"

        val request: Request = Request.RequestBuilder.create().setUrl(url)
                .setHeaders(headers)
                .mustSendContentEncoding(false)
                .build()
        val response: Response = dataFetcher.get(session, request)

        val body = CrawlerUtils.stringToJson(response.body)

        return body["id"] as String
    }

    private fun isProductPage(document: Document): Boolean {
        return document.selectFirst(".product_name") != null
    }

    private fun print(string: Any?) {
        println(">>> ✅ $string")
    }
}
