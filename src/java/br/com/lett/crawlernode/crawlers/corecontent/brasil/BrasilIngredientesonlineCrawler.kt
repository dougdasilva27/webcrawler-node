package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.MathUtils
import exceptions.MalformedPricingException
import models.Offer
import models.Offers
import models.pricing.CreditCard
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing
import org.apache.http.impl.cookie.BasicClientCookie
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Date: 21/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilIngredientesonlineCrawler(session: Session) : Crawler(session) {

    companion object {
        const val SELLER_NAME: String = "Ingredientes Online"
    }

    override fun fetch(): Any? {



        val cookie = BasicClientCookie("frontend", "vjn437tjgh4n511tjmnp9ncdj6")
        cookie.domain = ".www.ingredientesonline.com.br"
        cookie.path = "/"

        cookies.add(cookie)


        val request = Request.RequestBuilder.create().setCookies(cookies).setUrl(session.originalURL).build()
        val response = dataFetcher[session, request]
        val html = response.body
        return Jsoup.parse(html)
    }

    //2984
    override fun extractInformation(doc: Document): MutableList<Product> {
        super.extractInformation(doc)

        if (!isProductPage(doc)) {
            return mutableListOf()
        }

        val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-shop .product-name", false)
        val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li:not(:first-child):not(:last-child) a span")
        val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-shop input[name=product]", "value")
        val primaryImage = doc.selectFirst(".product-view .product-image img").attr("src")

        val secondaryImages = doc.select(".product-view .product-image div:not(:first-child) img").map { it.attr("src") }.toList()

        val description = doc.select("#descricao .contentDescWrap").first {
            it.attr("id") != "tabelanutricional"
        }.html()

        val offers = scrapOffers(doc)

        val product = ProductBuilder()
                .setUrl(session.originalURL)
                .setInternalId(internalId)
                .setName(name)
                .setCategories(categories)
                .setPrimaryImage(primaryImage)
                .setSecondaryImages(secondaryImages)
                .setDescription(description)
                .setOffers(offers)
                .setRatingReviews(null)
                .build()

        return mutableListOf(product)
    }

    private fun scrapOffers(doc: Document): Offers {
        val offers = Offers()

        val installments = scrapInstallments(doc)

        val priceText = doc.selectFirst(".old-price .price")?.text() ?: ""


        var priceFrom = MathUtils.parseDoubleWithComma(priceText)

        val spotlightText = if (priceFrom > 0) {
            doc.selectFirst(".special-price .price")?.text()
        } else {
            doc.selectFirst(".regular-price .price")?.text()
        } ?: ""


        val spotlightPrice = MathUtils.parseDoubleWithComma(spotlightText)

        spotlightPrice?.let {
            if (spotlightPrice == priceFrom) {
                priceFrom = null
            }

            val rennerCard = CreditCard.CreditCardBuilder.create()
                    .setBrand("Cartão Renner")
                    .setIsShopCard(true)
                    .setInstallments(
                            installments
                    )
                    .build()



            val creditCards = CreditCards(
                    listOf(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO,
                            Card.HIPERCARD).map { card: Card ->
                        val installment = CrawlerUtils
                                .crawlSimpleInstallment(".parcel", doc, false, "x", "juros", false, ',')
                        try {
                            return@map CreditCardBuilder.create()
                                    .setBrand(card.toString())
                                    .setIsShopCard(false)
                                    .setInstallments(Installments(setOf(InstallmentBuilder.create()
                                            .setInstallmentPrice(installment.second.toDouble())
                                            .setInstallmentNumber(installment.first)
                                            .build())))
                                    .build()
                        } catch (e: MalformedPricingException) {
                            throw RuntimeException(e)
                        }
                    })

            offers.add(
                    Offer.OfferBuilder.create()
                            .setPricing(
                                    Pricing.PricingBuilder.create()
                                            .setCreditCards(creditCards)
                                            .setSpotlightPrice(spotlightPrice)
                                            .setPriceFrom(priceFrom)
                                            .build()
                            )
                            .setSales(listOf())
                            .setIsMainRetailer(true)
                            .setIsBuybox(false)
                            .setUseSlugNameAsInternalSellerId(true)
                            .setSellerFullName(SELLER_NAME)
                            .build()
            )
        }

        return offers
    }

    private fun scrapInstallments(doc: Document): Installments {
        val installments = Installments()
        doc.select(".preco-comprar .table-pagamento ul li").map {

            val text = it.text()

            val pair = CrawlerUtils.crawlSimpleInstallmentFromString(text, "de", "sem juros", true)

            val installment = InstallmentBuilder()
                    .setInstallmentNumber(pair.first)
                    .setInstallmentPrice(pair.second.round())
                    .setFinalPrice((pair.first * pair.second).round())
                    .build()

            installments.add(installment)
        }
        return installments
    }

    private fun isProductPage(document: Document): Boolean {
        return document.selectFirst(".product-name") != null
    }
}

fun print(str: Any?) {
    println(">>> ✅ $str")
}

fun Double.round(): Double {
    return ((this * 100).toInt()).toDouble() / 100
}

fun Float.round(): Double {
    return this.toDouble().round()
}
