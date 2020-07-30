package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer
import models.Offers
import models.pricing.Installments
import models.pricing.Pricing
import org.jsoup.nodes.Document

class BrasilEtnamoveisCrawler(session: Session?) : Crawler(session) {

   init {
      config.fetcher = FetchMode.FETCHER
   }

   private val homePage = "https://www.etna.com.br"
   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith(homePage)
   }

   val scrapedVariations = mutableListOf<String>()

   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products = mutableListOf<Product>()
      if (doc.selectFirst("#js-page-details-container") != null) {
         val images = doc.select(".zoomIt img").map { homePage + it.attr("data-zoom-image") }
         val offers: Offers = scrapOffers(doc)
         products += CrawlerUtils.scrapSchemaOrg(doc)
            .setPrimaryImage(images.first())
            .setUrl(session.originalURL)
            .setCategories(doc.select(".breadcrumb li").eachText(arrayOf(0)))
            .setSecondaryImages(images.sliceFirst())
            .setOffers(offers)
            .build()
      }
      val variations = doc.select(".form-control.etn-select--custom.variant-select option")?.sliceFirst()
      if (variations != null) {
         scrapedVariations addNonNull doc.selectFirst("#currentSizeValue")?.attr("data-size-value")
         variations
            .filter { it.text().trim() !in scrapedVariations }
            .forEach {
               val body = dataFetcher.get(session, Request.RequestBuilder.create().setUrl(homePage + it.attr("value")!!).build()).body?.toDoc()
               if (body != null) products += extractInformation(body)
            }
      }
      return products
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()
      val price = doc.selectFirst(".etn-price__list")?.toDoubleComma()
      val priceFrom = doc.selectFirst(".etn-price__old")?.toDoubleComma()
      val installments = Installments(setOf(doc.installment(".easy-installment")))
      val pricing = Pricing.PricingBuilder
         .create()
         .setSpotlightPrice(price)
         .setCreditCards(listOf(MASTERCARD, VISA, AMEX, ELO, HIPERCARD, DINERS).toCreditCards(installments))
         .setBankSlip(price?.toBankSlip())
         .setPriceFrom(priceFrom).build()
      val sellerName = doc.selectFirst(".etn-product__delivery strong")?.text()
      offers.add(
         Offer.OfferBuilder
            .create()
            .setPricing(pricing)
            .setSellerFullName(sellerName)
            .setUseSlugNameAsInternalSellerId(true)
            .setIsBuybox(false)
            .setIsMainRetailer(sellerName?.matches("Etna".toRegex()) ?: true)
            .build()
      )
      return offers
   }
}
