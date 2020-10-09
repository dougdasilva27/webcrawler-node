package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import models.prices.Prices
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilNeiCrawler(session: Session?) : Crawler(session) {

   override fun extractInformation(document: Document?): MutableList<Product> {
      val products = mutableListOf<Product>()

      val json = CrawlerUtils.selectJsonFromHtml(document, "script[type='text/javascript']", "window.dataLayer.push(", ");", false, true)
      val jsonProduct = json?.optJSONObject("ecommerce")?.optJSONObject("detail")

      val ratingId = json?.optJSONObject("remarketing")?.optString("ecomm_prodid")

      json ?: Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)

      jsonProduct?.let {
         for (any in it.optJSONArray("products")) {
            if (any is JSONObject) {

               val price = any.optFloat("original_price")
               val prices = scrapPrices(price)
               val internalId = any.optString("id")
               products.add(
                  ProductBuilder.create()
                     .setUrl(session.originalURL)
                     .setInternalId(internalId)
                     .setName(any.optString("name"))
                     .setDescription(
                        CrawlerUtils.scrapElementsDescription(
                           document, mutableListOf(
                              ".product-main--description",
                              ".product-details > *:not(#trustvox-reviews):not(#_sincero_widget):not(script)"
                           )
                        )
                     )
                     .setPrice(if (price == 0F) null else price)
                     .setPrices(if (price == 0F) null else prices)
                     .setRatingReviews(scrapRating(ratingId = ratingId, doc = document))
                     .setAvailable(document?.selectFirst(".formAddCart--button") != null)
                     .setCategories(CrawlerUtils.crawlCategories(document, "ul.breadcrumbs li a span", true))
                     .setPrimaryImage(any.optString("image"))
                     .build()
               )
            }
         }
      }

      return products
   }

   private fun scrapPrices(price: Float): Prices {
      val prices = Prices()
      prices.bankTicketPrice = price.toDouble()
      val installments: MutableMap<Int, Float> = mutableMapOf()
      with(prices) {
         installments[1] = price
         insertCardInstallment(Card.VISA.toString(), installments)
         insertCardInstallment(Card.MASTERCARD.toString(), installments)
         insertCardInstallment(Card.ELO.toString(), installments)
      }
      return prices
   }

   private fun scrapRating(ratingId: String?, doc: Document?) = TrustvoxRatingCrawler(session, "106552", logger)
      .extractRatingAndReviews(ratingId, doc, dataFetcher).also { it.date = session.date }
}
