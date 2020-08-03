package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import exceptions.MalformedPricingException
import models.Offer
import models.Offers
import models.pricing.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Date: 03/08/20
 *
 * @author Fellype Layunne
 *
 */

class BrasilGirafaCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Girafa"
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto .detalhes-produto .titulo-produto", false)

      val categories = scrapCategories(doc)

      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".favoritar", "data-id")

      val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".produto .fotos-miniaturas img", "data-large")

      val secondaryImages = doc.select(".produto .fotos-miniaturas img:not(:first-child)").eachAttr("data-large")

      val description = doc.htmlOf(".container .descricao_completa")

      val offers = scrapOffers(doc)

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
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

   private fun scrapCategories(doc: Document): CategoryCollection {
      val categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a:not(:first-child)")

      val lastCategory = CrawlerUtils.scrapStringSimpleInfo(doc, ".breadcrumb", true)

      categories addNonNull lastCategory

      return categories
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val installments = scrapInstallments(doc)

      val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precos .risco-produto", null, false, ',', session)

      val spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precos .desconto-produto", null, false, ',', session)

      spotlightPrice?.let {

         val creditCards = listOf(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO).toCreditCards(installments)

         val bankSlipDiscount = CrawlerUtils.scrapSimpleInteger(doc, ".produto .precos .laranja", false).toDouble()/100

         val bankSlip : BankSlip? = BankSlip.BankSlipBuilder()
            .setFinalPrice(spotlightPrice)
            .setOnPageDiscount(bankSlipDiscount)
            .build()

         val sales = doc.select(".produto .precos .desconto").eachText()

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

   private fun scrapInstallments(doc: Document): Installments {

      val installments = mutableListOf<Installment>()

      val discount = CrawlerUtils.scrapSimpleInteger(doc, ".produto .precos .laranja", false)

      for (e: Element in doc.select(".modal-content .parcelas-table tr")) {
         val a1 = CrawlerUtils.scrapSimpleInteger(e.child(0), "td", false)


         val a2 = CrawlerUtils.scrapDoublePriceFromHtml(e.child(1), null, null, false, ',', session)
         val a3 = CrawlerUtils.scrapDoublePriceFromHtml(e.child(2), null, null, false, ',', session)

         val onPageDiscount = if (a1 == 1) {
            discount.toDouble()/100
         } else {
            null
         }

         val installment = Installment.InstallmentBuilder()
            .setInstallmentNumber(a1)
            .setInstallmentPrice(a2)
            .setFinalPrice(a3)
            .setOnPageDiscount(onPageDiscount)
            .build()

         installments += installment


      }

      installments.sortBy { it.installmentNumber }

      return Installments(installments.toSet())
   }

   private fun isProductPage(document: Document): Boolean {
      return document.selectFirst(".produto .detalhes-produto") != null
   }
}
