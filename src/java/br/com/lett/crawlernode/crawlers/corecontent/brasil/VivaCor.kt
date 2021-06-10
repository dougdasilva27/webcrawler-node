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
import models.pricing.Installments
import models.pricing.Pricing
import org.jsoup.nodes.Document

class VivaCor(session: Session) : Crawler(session) {


   override fun extractInformation(document: Document): MutableList<Product> {
      var p = mutableListOf<Product>()
      val internalPid = document.selectFirst("input[name=product_id]").attr("value")
      val name = "${document.selectFirst("#details-product .title").text()} ${document.selectFirst(".text_manufacturer_name a").text()}"
      val offers = if (document.selectFirst("#notify") == null) scrapOffers(document) else Offers()

      val product = ProductBuilder()
         .setUrl(session.originalURL)
         .setInternalId(internalPid)
         .setInternalPid(internalPid)
         .setName(name)
         .setCategories(document.select(".breadcrumb li").eachText(arrayOf(0)))
         .setPrimaryImage(document.selectFirst("#preview").attr("data-zoom-image"))
         .setDescription(document.selectFirst("#box-description").text())
         .setOffers(offers)
         .build()

      p.add(product)

      if (document.selectFirst(".list-inline.product_options_list") != null) {
         p = scrapVariations(document, product)
      }

      return p
   }

   private fun scrapVariations(doc: Document, mainProduct: Product): MutableList<Product> {
      val variations = doc.select(".list-inline.product_options_list li")
      return variations.map { variation ->
         val product = mainProduct.clone()
         product.name = product.name + " " + variation.attr("title")
         product.internalId = variation.attr("data-product-option-value-id")
         val optionPrice = if (product.internalId == product.internalPid) "0" else
            variation.attr("data-content").substringAfter("|").substringBefore(".")
         product.offers?.offersList?.firstOrNull()?.pricing = scrapVariationPricing(product.internalPid, optionPrice)
         product
      }.toMutableList()
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(scrapPricing(doc))
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Viva Cor")
            .build()
      )
      return offers
   }

   private fun scrapVariationPricing(productId: String, optionPrice: String): Pricing {

      val req = Request.RequestBuilder.create()
         .setHeaders(mapOf("content-type" to "application/x-www-form-urlencoded; charset=UTF-8"))
         .setUrl("https://www.vivacortintas.com/index.php?route=product%2Fproduct%2FpriceOption")
         .setPayload("product_id=$productId&option_price=$optionPrice&product_quantity=1&rule_price_type=false").build()

      val resp = dataFetcher.post(session, req).body.toJson()

      val json = resp.optJSONObject("calculate_prices_output")
      val price = json.optString("price").toDoubleComma()
      val installments = Installments()

      val installment = (json.optInt("parcel_simulator") to json.optString("price_simulator")?.toDoubleComma()).installment()
      installments.add(installment)

      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS).toCreditCards(installments)
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .setBankSlip(price?.toBankSlip())
         .build()
   }

   private fun scrapPricing(doc: Document): Pricing {

      val price = doc.selectFirst(".price").toDoubleComma() ?: throw IllegalStateException("")
      val bankSlip = price.toBankSlip()
      val installments = Installments()
      installments.add(doc.installment(".price_simultador_product"))

      val creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.HIPER, Card.AMEX, Card.AURA, Card.ELO, Card.DINERS).toCreditCards(installments)
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build()
   }
}
