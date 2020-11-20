package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import models.Offer
import models.Offers
import models.RatingsReviews
import models.pricing.*
import models.pricing.Installment.InstallmentBuilder
import org.jsoup.nodes.Document
import java.util.*

class BrasilMegustavegCrawler(session: Session) : Crawler(session) {

    private val BASE_URL: String = "www.megustaveg.com.br"
    private val SELLER_FULL_NAME: String = "Me Gusta Veg"
    private val cards = listOf(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString())

    override fun extractInformation(doc: Document): MutableList<Product> {
        super.extractInformation(doc)
        val products = mutableListOf<Product>()

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified: ${session.originalURL}")
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".acoes-produto", "data-produto-id")
            val internalPid = internalId
            val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-produto", true)
            val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > ul > li", true)
            val description = CrawlerUtils.scrapElementsDescription(doc, listOf(".conteudo"))
            val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbs-vertical li > a", listOf("data-imagem-grande"), "http", "cdn.awsli.com.br")
            val secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbs-vertical li > a", listOf("data-imagem-grande"), "http", "cdn.awsli.com.br", primaryImage)
            val availability = doc.selectFirst(".disponivel") != null
            val stock = CrawlerUtils.scrapIntegerFromHtml(doc, ".qtde_estoque", true, null)
            val offers = if (availability) scrapOffers(doc) else Offers()
            val ratingsReviews = scrapRating(internalId, doc)

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
                    .setStock(stock)
                    .setRatingReviews(ratingsReviews)
                    .build()
            products.add(product)

        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
        }

        return products
    }

    private fun isProductPage(doc: Document): Boolean {
        return doc.selectFirst(".produto") != null
    }

    private fun scrapOffers(doc: Document): Offers {
        val offers = Offers()
        val pricing: Pricing = scrapPricing(doc)
        val sales: List<String> = ArrayList()

        offers.add(Offer.OfferBuilder.create()
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

    private fun scrapPricing(doc: Document): Pricing {
        val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".acoes-produto  .preco-promocional", null, true, ',', this.session)
        val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".acoes-produto .preco-venda", null, true, ',', this.session)

        return Pricing.PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setBankSlip(scrapBankSlip(doc))
                .setCreditCards(scrapCreditCards(doc, spotlightPrice))
                .build()
    }

    private fun scrapCreditCards(doc: Document, spotlightPrice: Double): CreditCards {
        val creditCards = CreditCards()
        val installments = scrapInstallments(doc)

        if (installments.installments.isEmpty()) {
            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(1)
                    .setInstallmentPrice(spotlightPrice)
                    .build())
        }

        for (brand in cards) {
            creditCards.add(CreditCard.CreditCardBuilder.create()
                    .setBrand(brand)
                    .setIsShopCard(false)
                    .setInstallments(installments)
                    .build())
        }

        return creditCards
    }

    private fun scrapInstallments(doc: Document): Installments {
        val installments = Installments()
        val items = doc.select(".parcela")

        for (row in items) {
            val number = CrawlerUtils.scrapIntegerFromHtml(row, "b", true, null)
            val price = CrawlerUtils.scrapDoublePriceFromHtml(row, "span", null, true, ',', session)

            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(number)
                    .setInstallmentPrice(price)
                    .build())
        }
        return installments
    }

    private fun scrapBankSlip(doc: Document): BankSlip {
        val price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "b.text-parcelas", null, true, ',', this.session)
        return BankSlip.BankSlipBuilder.create()
                .setFinalPrice(price)
                .build()
    }

    private fun scrapRating(internalId: String, doc: Document): RatingsReviews {
        val trustVox = TrustvoxRatingCrawler(session, "71474", logger)
        return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher)
    }
}