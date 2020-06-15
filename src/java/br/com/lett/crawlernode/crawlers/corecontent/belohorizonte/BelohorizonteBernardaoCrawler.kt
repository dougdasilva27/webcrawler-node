package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import exceptions.MalformedPricingException
import exceptions.OfferException
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.BankSlip.BankSlipBuilder
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document

class BelohorizonteBernardaoCrawler(session: Session?) : Crawler(session) {

    private val BASE_URL = "www.bernardaoemcasa.com.br"
    private val MAIN_SELLER_NAME = "Bernardão";

    private val cards = listOf(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.ELO.toString())


    override fun extractInformation(doc: Document): MutableList<Product> {
        super.extractInformation(doc)
        val products = mutableListOf<Product>()

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified:  ${session.originalURL}")

            val internalPid = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".no-display input[name=product]", "value", 0)
            val internalId = internalPid
            val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", true)
            val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li > :not(span)", true)
            val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,
                    ".rsImg[href]", listOf("href", "content", "src"), "https://", BASE_URL)
            val secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
                    ".rsImg[href]", listOf("href", "content", "src"), "https://", BASE_URL, primaryImage)
            val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".std"))
            val availability = scrapAvailability(doc)

            val offers = if (availability) scrapOffers(doc) else Offers()

            val product = ProductBuilder.create()
                    .setUrl(session.originalURL)
                    .setInternalId(internalId.toString())
                    .setInternalPid(internalPid.toString())
                    .setName(name)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(secondaryImages)
                    .setDescription(description)
                    .setOffers(offers)
                    .build()

            products.add(product)
        } else {
            Logging.printLogDebug(logger, session, "Not a product page ${session.originalURL}")
        }

        return products
    }

    private fun isProductPage(doc: Document): Boolean {
        return doc.select(".product") != null
    }

    private fun scrapAvailability(doc: Document): Boolean {
        return doc.selectFirst(".in-stock") != null
    }

    private fun scrapOffers(doc: Document): Offers {
        val offers = Offers()
        val pricing = scrapPricing(doc)

        if (pricing != null) {
            offers.add(OfferBuilder.create()
                    .setUseSlugNameAsInternalSellerId(true)
                    .setSellerFullName(MAIN_SELLER_NAME)
                    .setSellersPagePosition(1)
                    .setIsBuybox(false)
                    .setIsMainRetailer(true)
                    .setPricing(pricing)
                    .build())
        }
        return offers
    }

    private fun scrapPricing(doc: Document): Pricing? {
        val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".regular-price span", null, true, ',', session)

        if (spotlightPrice != null) {
            return PricingBuilder.create()
                    .setPriceFrom(null)
                    .setSpotlightPrice(spotlightPrice)
                    .setCreditCards(scrapCreditCards(spotlightPrice))
                    .setBankSlip(BankSlipBuilder.create().setFinalPrice(null).build())
                    .build()
        }
        return null
    }

    private fun scrapCreditCards(spotlightPrice: Double): CreditCards {
        val creditCards = CreditCards()
        val installments: Installments = scrapInstallments(spotlightPrice)
        if (installments.installments.isEmpty()) {
            installments.add(
                    InstallmentBuilder.create()
                            .setInstallmentNumber(1)
                            .setInstallmentPrice(spotlightPrice)
                            .build())
        }
        for (card in cards) {
            creditCards.add(
                    CreditCardBuilder.create()
                            .setBrand(card)
                            .setInstallments(installments)
                            .setIsShopCard(false).build())
        }
        return creditCards
    }

    private fun scrapInstallments(spotlightPrice: Double): Installments {
        val installments = Installments()
        installments.add(
                InstallmentBuilder.create()
                        .setInstallmentNumber(1)
                        .setInstallmentPrice(spotlightPrice)
                        .build())
        return installments
    }
}