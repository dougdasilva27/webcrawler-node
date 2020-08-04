package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.addNonNull
import br.com.lett.crawlernode.util.htmlOf
import br.com.lett.crawlernode.util.toCreditCards
import models.Offer
import models.Offers
import models.pricing.BankSlip
import models.pricing.Installment
import models.pricing.Installments
import models.pricing.Pricing
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

      val discount = CrawlerUtils.scrapSimpleInteger(doc, ".produto .precos .laranja", false).toDouble()/100

      for (e: Element in doc.select(".modal-content .parcelas-table tr")) {
         val installmentNumber
            = CrawlerUtils.scrapSimpleInteger(e.child(0), "td", false)


         val installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(e.child(1), null, null, false, ',', session)
         val finalPrice = CrawlerUtils.scrapDoublePriceFromHtml(e.child(2), null, null, false, ',', session)

         val onPageDiscount = if (installmentNumber == 1) {
            discount
         } else {
            null
         }

         val installment = Installment.InstallmentBuilder()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentPrice)
            .setFinalPrice(finalPrice)
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
