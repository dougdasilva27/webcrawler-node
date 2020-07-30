package br.com.lett.crawlernode.crawlers.corecontent.belgium

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
import org.json.JSONObject
import org.jsoup.nodes.Document

/**
 * Date: 30/07/20
 *
 * @author Fellype Layunne
 *
 */

class BelgiumDelhaizeCrawler(session: Session) : Crawler(session) {

   companion object {
      const val SELLER_NAME: String = "Delhaize"
   }

   override fun extractInformation(doc: Document): MutableList<Product> {
      super.extractInformation(doc)

      if (!isProductPage(doc)) {
         return mutableListOf()
      }

      val name = doc.selectFirst(".product-details .page-title")?.text()
      val internalId = doc.selectFirst(".ProductDetails .ProductBasketManager .ProductBasketAdder").attr("data-product-id")

      val description = doc.selectFirst(".ShowMoreLess__content")?.html()

      val categories = scrapCategories(doc)

      val primaryImage = scrapPrimaryImage(doc)
      val secondaryImages = scrapSecondaryImages(doc)

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

   private fun scrapPrimaryImage(doc: Document): String {
      val data = doc.selectFirst(".magnifyWrapper div").attr("data-media")

      val json = JSONUtils.stringToJson(data)

      json?.keys()?.forEach {
         val image = json.optString(it)
         if (image?.isNotEmpty() == true) {
            return "https:${image}"
         }
      }
      return ""
   }

   private fun scrapSecondaryImages(doc: Document): List<String> {
      val images = mutableListOf<String>()

      doc.select(".magnifyWrapper div").filterIndexed{ i, _ -> i>0}.map { it ->

         val data = it.attr("data-media")

         val json = JSONUtils.stringToJson(data)

         json?.keys()?.forEach {
            val image = json.optString(it)
            if (image?.isNotEmpty() == true){
               images += "https:${image}"
            }
         }
      }
      return images
   }

   private fun scrapCategories(doc: Document): Collection<String> {
      return doc.select(".Breadcrumb a:not(.home)").eachAttr("title", arrayOf(0))
   }

   private fun scrapOffers(doc: Document): Offers {
      val offers = Offers()

      val priceText = doc.selectFirst(".ProductDetails .ultra-bold.test-price-property")?.text() ?: ""

      val price = MathUtils.parseDoubleWithComma(priceText)

      val sales = doc.select(".ProductDetails .ProductPromotions .text-bold").map {
         it.text()
      }
      offers.add(
         Offer.OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(price)
                  .build()
            )
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .setSales(sales)
            .build()
      )

      return offers
   }

   private fun isProductPage(document: Document): Boolean {
      return document.selectFirst(".product-details") != null
   }
}
