package br.com.lett.crawlernode.crawlers.corecontent.campinas

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import models.AdvancedRatingReview
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.BankSlip.BankSlipBuilder
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing
import models.pricing.Pricing.PricingBuilder
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

class CampinasPetcampCrawler(session: Session) : Crawler(session) {

    private val SELLER_FULL_NAME = "petcamp"
    private val cards = listOf(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString())

    override fun extractInformation(doc: Document): MutableList<Product> {
        super.extractInformation(doc)
        val products = mutableListOf<Product>()

        val json = CrawlerUtils
                .selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] =", ";", false, true)

        if (json != null && json.has("id")) {
            Logging.printLogDebug(logger, session, "Product page identified: ${session.originalURL}")
            val internalId = json.optString("id")
            val internalPid = internalId
            val name = CrawlerUtils.scrapStringSimpleInfo(doc, "#lblNome", true)

            val categories = CrawlerUtils.crawlCategories(doc, ".migalha > :not(div)")
            val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".descricao_texto_conteudo"))
            val primaryImage = crawlPrimaryImage(doc)
            val secondaryImages = crawlSecondaryImages(doc)

            val jsonArraySku: JSONArray = if (json.optJSONArray("variants") != null) json.optJSONArray("variants") else JSONArray()
            val jsonSku = jsonArraySku.optJSONObject(0)
            val ean = json.optString("ean")
            val eans = if (ean != null && ean.isNotEmpty()) listOf(ean) else null
            val offers = if (jsonSku.optBoolean("available")) scrapOffers(jsonSku, doc) else Offers()
            val ratingsReviews = scrapRatingReviews(doc)

            val product = ProductBuilder.create()
                    .setUrl(session.originalURL)
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setOffers(offers)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(secondaryImages)
                    .setDescription(description)
                    .setRatingReviews(ratingsReviews)
                    .setEans(eans)
                    .build()
            products.add(product)

        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
        }

        return products
    }

    private fun crawlPrimaryImage(doc: Document): String? {
        val images = doc.selectFirst("#thumblist li > a")
        val rel = CrawlerUtils.stringToJson(images.attr("rel"))

        return if (rel.optString("largeimage") != null) {
            rel.optString("largeimage")
        } else if (rel.optString("smallimage") != null) {
            rel.optString("smallimage")
        } else null
    }

    private fun crawlSecondaryImages(doc: Document): String? {
        var secondaryImages: String? = null
        val secondaryImagesArray = JSONArray()
        val images = doc.select("#thumblist li:not(:first-child) > a")
        for (e in images) {
            val rel = CrawlerUtils.stringToJson(e.attr("rel"))
            secondaryImagesArray.put(if (rel.optString("largeimage") != null) {
                rel.optString("largeimage")
            } else if (rel.optString("smallimage") != null) {
                rel.optString("smallimage")
            } else null)
        }
        if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString()
        }
        return secondaryImages
    }

    private fun scrapOffers(json: JSONObject, doc: Document): Offers {
        val offers = Offers()
        val pricing: Pricing = scrapPricing(json, doc)
        val sales: List<String> = ArrayList()

        offers.add(OfferBuilder.create()
                .setUseSlugNameAsInternalSellerId(true)
                .setSellerFullName(SELLER_FULL_NAME)
                .setMainPagePosition(1)
                .setIsBuybox(false)
                .setIsMainRetailer(true)
                .setPricing(pricing)
                .setSales(sales)
                .build())
        return offers
    }

    private fun scrapPricing(json: JSONObject, doc: Document): Pricing {
        val isOnSale = json.optBoolean("isOnSale")
        val spotlightPrice: Double?
        val priceFrom: Double?

        if (isOnSale) {
            spotlightPrice = json.optDouble("salePrice")
            priceFrom = json.optDouble("price")
        } else {
            spotlightPrice = json.optDouble("price")
            priceFrom = null
        }

        return PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setBankSlip(BankSlipBuilder.create()
                        .setFinalPrice(spotlightPrice)
                        .build())
                .setCreditCards(scrapCreditCards(doc, spotlightPrice))
                .build()
    }

    private fun scrapCreditCards(doc: Document, spotlightPrice: Double): CreditCards {
        val creditCards = scrapCreditCardsInPage(doc)
        val installments = Installments()

        if (creditCards.creditCards.isEmpty()) {
            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(1)
                    .setInstallmentPrice(spotlightPrice)
                    .build())

            for (brand in cards) {
                creditCards.add(CreditCardBuilder.create()
                        .setBrand(brand)
                        .setIsShopCard(false)
                        .setInstallments(installments)
                        .build())
            }
        }
        return creditCards
    }

    private fun scrapCreditCardsInPage(doc: Document): CreditCards {
        val creditCards = CreditCards()
        val installments = Installments()

        val banks = doc.select(".conteudo ul li")
        for (line in banks) {
            for (row in banks.select(".formas_parc tbody > tr")) {

                val currentCard = checkCreditCard(line)
                if (currentCard != null) {
                    val number = CrawlerUtils.scrapIntegerFromHtml(row, "td:first-child", true, null)
                    val price = CrawlerUtils.scrapDoublePriceFromHtml(row, "td:last-child", null, true, ',', session)

                    installments.add(InstallmentBuilder.create()
                            .setInstallmentNumber(number)
                            .setInstallmentPrice(price)
                            .build())

                    creditCards.add(CreditCardBuilder.create()
                            .setBrand(currentCard)
                            .setInstallments(installments)
                            .setIsShopCard(false)
                            .build())
                }
            }
        }
        return creditCards
    }

    private fun checkCreditCard(line: Element): String? {
        val url = CrawlerUtils.scrapStringSimpleInfoByAttribute(line, ".img_parc > img", "src")

        return when {
            url.contains("amex") -> {
                Card.AMEX.toString()
            }
            url.contains("aura") -> {
                Card.AURA.toString()
            }
            url.contains("elo") -> {
                Card.ELO.toString()
            }
            url.contains("visa") -> {
                Card.VISA.toString()
            }
            url.contains("master") -> {
                Card.MASTERCARD.toString()
            }
            url.contains("diners") -> {
                Card.DINERS.toString()
            }
            else -> null
        }
    }

    private fun scrapRatingReviews(doc: Document): RatingsReviews? {
        val ratingReviews = RatingsReviews()
        ratingReviews.date = session.date
        val totalNumOfEvaluations = doc.select(".comentarios .comentarios_realizados > li").size
        val advancedRatingReview: AdvancedRatingReview = scrapAdvancedRatingReview(doc)
        val avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview)

        ratingReviews.setTotalRating(totalNumOfEvaluations)
        ratingReviews.averageOverallRating = avgRating
        ratingReviews.totalWrittenReviews = totalNumOfEvaluations
        ratingReviews.advancedRatingReview = advancedRatingReview
        return ratingReviews
    }

    private fun scrapAdvancedRatingReview(doc: Document): AdvancedRatingReview {
        var star1 = 0
        var star2 = 0
        var star3 = 0
        var star4 = 0
        var star5 = 0

        val reviews = doc.select(".comentarios .comentarios_realizados > li")
        for (review in reviews) {
            when (review.select("div >  img[src*=\"estrela_on\"]").size) {
                1 -> star1 += 1
                2 -> star2 += 1
                3 -> star3 += 1
                4 -> star4 += 1
                5 -> star5 += 1
            }
        }
        return AdvancedRatingReview.Builder()
                .totalStar1(star1)
                .totalStar2(star2)
                .totalStar3(star3)
                .totalStar4(star4)
                .totalStar5(star5)
                .build()
    }
}