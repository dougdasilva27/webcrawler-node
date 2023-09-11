package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher
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
         .setProxyservice(listOf(ProxyCollection.BUY, ProxyCollection.LUMINATI_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build()

      val response = CrawlerUtils.retryRequest(request, session, JsoupDataFetcher(), true);


      return response.body?.toDoc() ?: Document(url)
   }

   override fun extractInformation(doc: Document): MutableList<Product> {

      if (!isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
         return mutableListOf()
      }

      val name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.dgf-titulo", true)
      val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ID_SubProduto", "value")
      val primaryCategory = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#CategoriaProduto", "value")
      val secondaryCategory = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#SubCategoriaProduto", "value")
      val primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".img-zoom #imgProduto", "src")
      val secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".jsThumbProduto", Arrays.asList("data-image"), "https", "www.farmadireta.com.br", primaryImage)
      val description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("section.dgf-single-abas"))
      val available = doc.select(".produto-esgotado-avise").isEmpty()
      val offers = if (available) scrapOffers(doc) else Offers();

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalId)
         .setInternalPid(internalId)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setCategories(mutableListOf(primaryCategory, secondaryCategory))
         .setOffers(offers)
         .setDescription(description)
         .build()

      return mutableListOf(product)
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.getElementById("ID_SubProduto") != null
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val offersElement = doc.select(".dgf-single-info")

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

            offers.add(
               Offer.OfferBuilder.create()
                  .setPricing(pricing)
                  .setIsMainRetailer(true)
                  .setIsBuybox(true)
                  .setUseSlugNameAsInternalSellerId(true)
                  .setSellerFullName(SELLER_NAME)
                  .setSales(scrapSales(offer))
                  .build()
            )
         }
      } else {
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

         offers.add(
            Offer.OfferBuilder.create()
               .setPricing(pricing)
               .setIsMainRetailer(true)
               .setIsBuybox(true)
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(SELLER_NAME)
               .setSales(null)
               .build()
         )
      }

      return offers
   }

   private fun scrapSales(offerElement: Element): List<String> {
      val sales: MutableList<String> = ArrayList()
      val salesDiscount = CrawlerUtils.scrapStringSimpleInfo(offerElement, "div .dgf-tag", false)?.substringBefore('%') ?: "0"
      sales.add(salesDiscount)
      return sales
   }
}
