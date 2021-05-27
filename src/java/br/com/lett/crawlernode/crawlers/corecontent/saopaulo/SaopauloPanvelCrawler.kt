package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import models.prices.Prices
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaLagallegaCrawler
import br.com.lett.crawlernode.util.*
import models.*
import models.pricing.Pricing
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.TreeMap

class SaopauloPanvelCrawler(session: Session?) : Crawler(session) {
   private val HOME_PAGE_HTTP = "http://www.panvel.com/"
   private val HOME_PAGE_HTTPS = "https://www.panvel.com/"

   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products: MutableList<Product> = ArrayList()
      if (isProductPage(doc)) {
         val internalId = doc.selectFirst(".codigo").text().substringAfter(": ").substringBefore(")")
         val categories = doc.select(".breadcrumb a span").eachText(arrayOf(0)).map { it.replace("/ ", "") }
         val primaryImage = crawlPrimaryImage(doc)
         val secondaryImages = doc.select("#slideshow__thumbs img").eachAttr("src", arrayOf(0))
         val description = doc.selectFirst(".produto-info").wholeText()
         val name = doc.selectFirst(".nome").text()
         val offers = if (!doc.select(".btn.btn-primary").isEmpty()) scrapOffers(doc) else null

         val product = ProductBuilder.create()
            .setUrl(session.originalURL)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build()
         products.add(product)
      }

      return products
   }

   private fun scrapOffers(doc: Document): Offers {
      val price = doc.selectFirst(".item-price__value.text-bold").toDoubleComma()!!
      val priceFrom = doc.selectFirst(".item-price__value.item-price__value--old").toDoubleComma()
      val bankSlip = price.toBankSlip()
      val creditCards = listOf(Card.HIPERCARD, Card.VISA, Card.MASTERCARD, Card.AMEX, Card.DINERS).toCreditCards(price)

      val offer = Offer.OfferBuilder.create()
         .setPricing(
            Pricing.PricingBuilder.create()
               .setSpotlightPrice(price)
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .setBankSlip(bankSlip)
               .build()
         )
         .setUseSlugNameAsInternalSellerId(true)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSellerFullName("Panvel")
         .build()

      return Offers(listOf(offer))
   }

   private fun isProductPage(doc: Document): Boolean {
      return doc.select(".container-produto").first() != null
   }

   private fun crawlPrice(product: JSONObject, dataLayer: JSONObject): Float? {
      var price: Float? = null
      if (product.has("price")) {
         price = CrawlerUtils.getFloatValueFromJSON(product, "price", true, false)
      } else if (dataLayer.has("productPrice") && !dataLayer.isNull("productPrice")) {
         price = CrawlerUtils.getFloatValueFromJSON(dataLayer, "productPrice", true, false)
      }
      return price
   }

   private fun crawlPrimaryImage(document: Document): String? {
      var primaryImageElement = document.select(".slideshow__slides div > img").first()
      if (primaryImageElement == null) {
         primaryImageElement = document.selectFirst(".slideshow__slides div img")
      }
      return primaryImageElement?.attr("src")

   }

   private fun crawlSecondaryImages(doc: Document): List<String> {
      var imagesElement = doc.select("#slideshow__thumbs img")
      return imagesElement.eachAttr("src", arrayOf(1))
   }

   private fun crawlPrices(price: Float?, product: JSONObject, dataLayer: JSONObject): Prices {
      val prices = Prices()
      if (price != null) {
         val installmentPriceMap: MutableMap<Int, Float> = TreeMap()
         installmentPriceMap[1] = price
         prices.setBankTicketPrice(price)
         if (product.has("old_price")) {
            prices.priceFrom = CrawlerUtils.getDoubleValueFromJSON(product, "old_price", true, false)
         } else {
            prices.priceFrom = CrawlerUtils.getDoubleValueFromJSON(dataLayer, "productPriceOriginal", true, false)
         }
         if (product.has("installment")) {
            val installment = product.getJSONObject("installment")
            if (installment.has("count") && installment.has("price")) {
               val textCount = installment["count"].toString().replace("[^0-9]".toRegex(), "")
               val textPrice = installment["price"].toString().replace("[^0-9.]".toRegex(), "")
               if (!textCount.isEmpty() && !textPrice.isEmpty()) {
                  installmentPriceMap[textCount.toInt()] = textPrice.toFloat()
               }
            }
         }
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap)
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap)
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap)
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap)
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap)
      }
      return prices
   }
}
