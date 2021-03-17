package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Pricing
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

class BrasilFarmadiretaCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Farma Direta"

      const val HOME_PAGE: String = "https://www.farmadireta.com.br/"

      val cards = listOf(
         Card.AMEX,
         Card.DINERS,
         Card.DISCOVER,
         Card.ELO,
         Card.HIPERCARD,
         Card.JCB,
         Card.MASTERCARD,
         Card.VISA,
      )
   }

   override fun fetch(): Document {
      val url = session.originalURL

      val request = Request.RequestBuilder.create()
         .setUrl(url)
         .build()

      val response = dataFetcher.get(session, request)

      return response.body?.toDoc() ?: Document(url)
   }

   //the product has no description
   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val name = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#NameProduto", "value")
      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ID_SubProduto", "value")
      val primaryCategory = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#CategoriaProduto", "value")
      val secondaryCategory = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#SubCategoriaProduto", "value")
      val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card .product-image-box > img[src]", "src")
      val description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ficha-produto"))
      val offers = scrapOffers(doc)

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setCategories(mutableListOf(primaryCategory, secondaryCategory))
         .setOffers(offers)
         .setDescription(description)
         .build()

      return mutableListOf(product)
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.getElementById("ID_SubProduto") != null
   }

   //the price is always there, even if the product is sold out
   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val offersElement = doc.select(".dgf-single-escolha > label")

      if (!offersElement.isEmpty()) {
         for (offer in offersElement) {
            val spotlightPrice: Double = CrawlerUtils.scrapDoublePriceFromHtml(offer, "span .por", null, false, ',', session)
            val bankSlip = spotlightPrice.toBankSlip()
            val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(offer, "span .de", null, false, ',', session)
            val installmentNumber = CrawlerUtils.scrapSimpleInteger(doc, "b > span.parcelas", false)
            val installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "b > span:not(.parcelas)", null, false, ',', session)
            val creditCards = cards.toCreditCards(installmentPrice, installmentNumber)

            val pricing = Pricing.PricingBuilder.create()
               .setSpotlightPrice(spotlightPrice)
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .setBankSlip(bankSlip)
               .build()

            offers.add(Offer.OfferBuilder.create()
                  .setPricing(pricing)
                  .setIsMainRetailer(true)
                  .setIsBuybox(true)
                  .setUseSlugNameAsInternalSellerId(true)
                  .setSellerFullName(SELLER_NAME)
                  .setSales(scrapSales(offer))
                  .build())
         }
      }else{
         val spotlightPrice: Double = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span .por", null, false, ',', session)
         val bankSlip = spotlightPrice.toBankSlip()
         val priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span .de", null, false, ',', session)
         val installmentNumber = CrawlerUtils.scrapSimpleInteger(doc, "b > span.parcelas", false)
         val installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "b > span:not(.parcelas)", null, false, ',', session)
         val creditCards = cards.toCreditCards(installmentPrice, installmentNumber)

         val pricing = Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build()

         offers.add(Offer.OfferBuilder.create()
            .setPricing(pricing)
            .setIsMainRetailer(true)
            .setIsBuybox(true)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .setSales(null)
            .build())
      }

      return offers
   }

   private fun scrapSales(offerElement: Element): List<String> {
      val sales: MutableList<String> = ArrayList()
      val salesDiscount = CrawlerUtils.scrapStringSimpleInfo(offerElement,"div .dgf-tag", false)?.substringBefore('%') ?: "0"
      sales.add(salesDiscount)
      return sales
   }
}
