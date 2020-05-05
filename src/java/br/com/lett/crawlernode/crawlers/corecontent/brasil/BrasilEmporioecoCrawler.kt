package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.BankSlip.BankSlipBuilder
import models.pricing.Pricing.PricingBuilder
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilEmporioecoCrawler(session: Session?) : Crawler(session) {
    override fun extractInformation(document: Document): MutableList<Product> {
        val products = mutableListOf<Product>()

        if (document.selectFirst(".row.la-single-product-page") == null) {
            Logging.printLogDebug(logger, session, "Not a product page: ${session.originalURL}")
            return products
        }

        val builder = ProductBuilder.create()
                .setUrl(session.originalURL)

        val json = CrawlerUtils.stringToJson(
                document.selectFirst("script[type='application/ld+json']").html())

        json?.let {
            builder.apply {
                setName(it.optString("name"))
                setInternalId(it.optString("sku"))
                setInternalPid(it.optString("sku"))
                setDescription(document.selectFirst("#tab-description").html())
                setCategories(document.select(".la-breadcrumb-item")
                        ?.toCategories(arrayOf(0, 1), arrayOf("/")))
                setPrimaryImage(it.optString("image"))
                setSecondaryImages(document.select(".woocommerce-product-gallery__wrapper a")
                        ?.toSecondaryImagesBy(ignoreIndex = arrayOf(0)))
            }

            for (jsonOffer in json.optJSONArray("offers")) {
                if (jsonOffer is JSONObject) {
                    val productBuilder = builder
                            .setOffers(scrapOffers(document, jsonOffer))

                    if (document.selectFirst(".variations_form.cart") != null) {
                        val variations = scrapVariations(document, productBuilder)

                        products.addAll(variations)
                    } else {
                        products.add(productBuilder
                                .build())
                    }
                }
            }
        }
        return products
    }

    private fun scrapVariations(document: Document, productBuilder: ProductBuilder): Collection<Product> {
        val products = mutableListOf<Product>()

        val jsonArray = document.selectFirst(".variations_form.cart").attr("data-product_variations").toJsonArray()

        val name = document.selectFirst(".product_title.entry-title").text()
        document.select("li.variable-item").forEachIndexed { index, element ->
            val jsonObject = jsonArray.optJSONObject(index)

            val product = productBuilder
                    .setInternalId(jsonObject.optString("variation_id"))
                    .setName("$name ${element.selectFirst("span").text()}")
                    .build()

            products.add(product)
        }

        return products
    }

    private fun scrapOffers(doc: Document, json: JSONObject): Offers {
        val offers = Offers()

        val price = json.optDouble("price")
        val priceFrom = doc.selectFirst(".price .woocommerce-Price-amount.amount").toDoubleComma()

        val creditCards = listOf(MASTERCARD, VISA, ELO, AMEX, HIPER, HIPERCARD).toCreditCards(price)

        offers.add(OfferBuilder.create()
                .setIsBuybox(false)
                .setPricing(PricingBuilder.create()
                        .setSpotlightPrice(price)
                        .setPriceFrom(if ((priceFrom != 0.0) and (priceFrom != price)) priceFrom else null)
                        .setBankSlip(BankSlipBuilder.create()
                                .setFinalPrice(price)
                                .build())
                        .setCreditCards(creditCards)
                        .build())
                .setSellerFullName(json.optJSONObject("seller")?.optString("name", null))
                .setIsMainRetailer(true)
                .setUseSlugNameAsInternalSellerId(true)
                .build())

        return offers
    }
}
