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
      val primaryImage = doc.selectFirst(".gallery-placeholder__image")?.attr("src")

      val secondaryImages = doc.select(".product-view .product-image div:not(:first-child) img").map { it?.attr("src") }
      .toMutableList().filterNotNull()//TODO

      val description = doc.select(".content .value p")?.html()

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

      val buyButton = doc.select("#product-addtocart-button span")?: null

      if (buyButton != null) {
         val installments = scrapInstallments(doc)

         val priceText = doc.selectFirst(".old-price .price")?.text()

         var priceFrom = if (priceText != null) {
            MathUtils.parseDoubleWithComma(priceText)
         } else {
            null
         }

         val spotlightText = if (priceFrom != null) {
            doc.selectFirst(".special-price .price")?.text()
         } else {
            doc.selectFirst(".product-info-price .price")?.text()
         }

         var spotlightPrice = spotlightText.toDoubleComma()?.round()

         println("spotlightPrice" + spotlightPrice)

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

            val bankSlipPrice = doc.selectFirst(".bankslip_excerpt .price")?.text().toDoubleComma()?.round()
            val bankSlipDiscountPercentage = doc.selectFirst(".bankslip_excerpt small")

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

      var parcel = doc.selectFirst(".installment_period") ?: 0
      val price = doc.selectFirst(".installment_value").toDoubleComma() ?: 0.0

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

   private fun isProductPage(document: Document): Boolean {
      return document.selectFirst(".product-name") != null
   }
}
