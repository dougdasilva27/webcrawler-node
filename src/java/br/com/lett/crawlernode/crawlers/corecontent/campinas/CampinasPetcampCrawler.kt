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


        if (!doc.select("#___rc-p-id").isEmpty()) {

            Logging.printLogDebug(logger, session, "Product page identified: ${session.originalURL}")
            val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"#___rc-p-id","value")
            val internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"#___rc-p-sku-ids","value")
            val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productName", true)

            val categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb")
            val description = CrawlerUtils.scrapSimpleDescription(doc, listOf(".productDescription"))
            val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,"#image a", Arrays.asList("href"),"https","petcamp.vteximg.com.br")

            val offers =  scrapOffers(doc)

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
                    .setDescription(description)
                    .build()
            products.add(product)

        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
        }

        return products
    }





    private fun scrapOffers(doc: Document): Offers {
        val offers = Offers()
        val pricing: Pricing = scrapPricing(doc)
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

    private fun scrapPricing(doc: Document): Pricing {
        val isOnSale = if(CrawlerUtils.scrapDoublePriceFromHtml(doc,".valor-de strong",null,false,',',session)!=null) true else false
        val spotlightPrice: Double?
        val priceFrom: Double?

        if (isOnSale) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,".valor-por strong",null,false,',',session)
            priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc,".valor-de strong",null,false,',',session)
        } else {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,".valor-por strong",null,false,',',session)
            priceFrom = null
        }

        return PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setBankSlip(BankSlipBuilder.create()
                        .setFinalPrice(spotlightPrice)
                        .build())
                .build()
    }

}
