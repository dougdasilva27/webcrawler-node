package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.MathUtils
import br.com.lett.crawlernode.util.round
import br.com.lett.crawlernode.util.toDoubleComma
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
      val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li:not(:first-child):not(:last-child) a span")//TODO
      val internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=sku]", false)
      val primaryImage = doc.selectFirst(".gallery-placeholder__image")?.attr("src")

      val secondaryImages = doc.select(".product-view .product-image div:not(:first-child) img").map { it?.attr("src") }
          .toMutableList().filterNotNull()//TODO
      println("secondaryImages "+ secondaryImages)
      println("\nsecondaryImages "+ primaryImage)
      println("\nsecondaryImages "+ categories)
      println("\nsecondaryImages "+ name)
      val description = doc.select(".content .value p")?.first {
         it.attr("id") != "tabelanutricional"
      }?.html()
      println("\ndescription "+ description)

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
         .build()

      return mutableListOf(product)
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

	   if(doc.select(".alert-stock").isEmpty()) { //#product-addtocart-button span
        val installments = scrapInstallments(doc)
  
        val priceText = doc.selectFirst(".old-price .price")?.text() ?: ""
  
        var priceFrom = MathUtils.parseDoubleWithComma(priceText)
  
        val spotlightText = if ((priceFrom ?: 0.0) > 0) {
           doc.selectFirst(".special-price .price")?.text()
        } else {
           doc.selectFirst(".regular-price .price")?.text()
        } ?: ""
  
        val spotlightPrice = MathUtils.parseDoubleWithComma(spotlightText)
  
        spotlightPrice?.let {
           if (spotlightPrice == priceFrom) {
              priceFrom = null
           }
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
  
           val bankSlipPrice = doc.selectFirst(".preco-comprar .boletoBox .price")?.text().toDoubleComma()?.round()
           val bankSlipDiscount = doc.selectFirst(".preco-comprar .boletoBox .descontoBoleto")?.text()?.toDouble()
  
           var bankSlip: BankSlip? = BankSlip.BankSlipBuilder()
              .setFinalPrice(bankSlipPrice)
  
              // convert from int to decimal percent
              .setOnPageDiscount(((bankSlipDiscount ?: 0.0) / 100).round())
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

      val parcel = doc.selectFirst(".parcelaBloco")?.attr("data-maximo_parcelas_sem_juros")?.toInt() ?: 0
      val price = doc.selectFirst(".parcelaBloco")?.attr("data-valor_produto")?.toDouble()?.round() ?: 0.0
      for (i: Int in 1..parcel) {
         val installment = InstallmentBuilder()
            .setInstallmentNumber(i)
            .setInstallmentPrice((price / i).round())
            .setFinalPrice(price)
            .build()

         installments.add(installment)
      }
      return installments
   }

   private fun isProductPage(document: Document): Boolean {
      return document.selectFirst(".product-name") != null
   }
}
