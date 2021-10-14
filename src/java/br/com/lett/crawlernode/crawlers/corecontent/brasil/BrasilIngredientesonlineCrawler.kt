package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricingException
import models.Offer
import models.Offers
import models.pricing.BankSlip
import models.pricing.CreditCard.CreditCardBuilder
import models.pricing.CreditCards
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*

class BrasilIngredientesonlineCrawler(session: Session) : Crawler(session) {
   companion object {
      const val SELLER_NAME: String = "Ingredientes Online"
   }

   init {
      config.fetcher = FetchMode.APACHE
   }

   override fun fetch(): Any? {
      val cookiesHome = dataFetcher.get(session, Request.RequestBuilder.create().setUrl("https://www.ingredientesonline.com.br/").build()).cookies
      cookies.addAll(cookiesHome)
      val request = Request.RequestBuilder.create().setCookies(cookies).setUrl(session.originalURL).build()
      val response = dataFetcher[session, request]
      val html = response.body
      return Jsoup.parse(html)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title", false)
      val internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=sku]", false)
      val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder__image", Arrays.asList("src"), "https", "")
      val secondaryImages = doc.select(".product-view .product-image div:not(:first-child) img").map { it?.attr("src") }
      .toMutableList().filterNotNull()
      val description = CrawlerUtils.scrapStringSimpleInfo(doc, ".content .value p", false)
      val offers = scrapOffers(doc)

      val product = ProductBuilder()
      .setUrl(session.originalURL)
      .setInternalId(internalId)
      .setName(name)
      .setPrimaryImage(primaryImage)
      .setSecondaryImages(secondaryImages)
      .setDescription(description)
      .setOffers(offers)
      .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val re = Regex("[^0-9]")

      val buyButton = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-addtocart-button span", false)?: null

      if (buyButton != null) {
         val installments = scrapInstallments(doc)

         val priceText = CrawlerUtils.scrapStringSimpleInfo(doc, ".old-price .price", false)

         var priceFrom = if (priceText == null) {
            null
         } else {
            MathUtils.parseDoubleWithComma(priceText)
         }

         var spotlightText = CrawlerUtils.scrapStringSimpleInfo(doc, ".normal-price .price", false)

         if (spotlightText == null) {
            spotlightText = CrawlerUtils.scrapStringSimpleInfo(doc, ".special-price .price", false)
         }

         var spotlightPrice = if (spotlightText.toDoubleComma()?.round() == null) {
            CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info-price .price", false).toDoubleComma()?.round()
         } else {
            spotlightText.toDoubleComma()?.round()
         }

         spotlightPrice.let {
            val creditCards = CreditCards(
               listOf(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO,
               Card.HIPERCARD).map { card: Card ->
               try {
                  return@map CreditCardBuilder.create()
                  .setBrand(card.toString())
                  .setIsShopCard(false)
                  .setInstallments(installments)
                  .build()
               } catch (e: MalformedPricingException) {
                  throw RuntimeException(e)
               }
            })
            val bankSlipPrice = CrawlerUtils.scrapStringSimpleInfo(doc, ".bankslip_excerpt .price", false).toDoubleComma()?.round()
            val bankSlipDiscountPercentage = CrawlerUtils.scrapStringSimpleInfo(doc, ".bankslip_excerpt small", false)

            var bankSlipDiscountValue = re.replace(bankSlipDiscountPercentage.toString(), "").toDouble()

            var bankSlip: BankSlip? = BankSlip.BankSlipBuilder()
            .setFinalPrice(bankSlipPrice)
            .setOnPageDiscount(((bankSlipDiscountValue) / 100)) // convert from int to decimal percent
            .build()

            if ((bankSlipPrice ?: 0.0) <= 0) {
               bankSlip = null
            }
               offers.add(
               Offer.OfferBuilder.create()
               .setPricing(
                  Pricing.PricingBuilder.create()
                  .setCreditCards(creditCards)
                  .setSpotlightPrice(spotlightPrice)
                  .setBankSlip(bankSlip)
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
      }
      return offers
   }

   private fun scrapInstallments(doc: Document): Installments {
      val installments = Installments()

      val parcel = CrawlerUtils.scrapStringSimpleInfo(doc, ".installment_period", false) ?: 0
      val price = CrawlerUtils.scrapStringSimpleInfo(doc, ".installment_value", false).toDoubleComma() ?: 0.0

      val re = Regex("[^0-9]")
      var parcelNumber = re.replace(parcel.toString(), "").toInt()

      val installment = InstallmentBuilder()
      .setInstallmentNumber(parcelNumber)
      .setInstallmentPrice(price.round())
      .setFinalPrice(parcelNumber * price)
      .build()

      installments.add(installment)

      return installments
   }

   private fun isProductPage(doc: Document): Boolean {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", false) != null
   }
}
