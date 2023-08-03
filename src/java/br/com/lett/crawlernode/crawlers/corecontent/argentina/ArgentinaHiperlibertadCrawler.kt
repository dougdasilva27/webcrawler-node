package br.com.lett.crawlernode.crawlers.corecontent.argentina

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.util.*
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.Pricing
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class ArgentinaHiperlibertadCrawler(session: Session) : VTEXOldScraper(session) {

   private val storeId: String = session.options.optString("storeId")

   override fun getHomePage(): String {
      return "https://www.hiperlibertad.com.ar/"
   }

   override fun fetch(): Any {
      if (storeId.isNotEmpty()) {
         val cookie = BasicClientCookie("VTEXSC", storeId)
         cookie.domain = "www.hiperlibertad.com.ar"
         cookie.path = "/"
         cookies.add(cookie)
      }
      val request = RequestBuilder.create().setUrl(session.originalURL).setCookies(cookies).build()
      return Jsoup.parse(dataFetcher[session, request].body)
   }

   override fun getMainSellersNames(): MutableList<String> {
      return mutableListOf("Hiper Liberdade", "LIBERTAD SA")
   }

   override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews? {
      return null
   }

   override fun extractInformation(doc: Document?): MutableList<Product> {
      val products: MutableList<Product> = ArrayList()

      val internalPid = scrapPidFromApi(doc)

      if (internalPid != null && isProductPage(doc)) {
         var productJson = crawlProductApi(internalPid, null)
         if (productJson!!.isEmpty) {
            val array = doc?.let { crawlSkuJsonArray(it) }
            productJson = array?.let { array.optJSONObject(0) }
         }
         val categories = scrapCategories(productJson)
         val description = scrapDescription(doc, productJson)
         processBeforeScrapVariations(doc, productJson, internalPid)
         if (productJson != null) {
            val items = JSONUtils.getJSONArrayValue(productJson, "items")
            if (items.length() > 0) {
               for (i in 0 until items.length()) {
                  var jsonSku = items.optJSONObject(i)
                  if (jsonSku == null) {
                     jsonSku = JSONObject()
                  }
                  val product = extractProduct(doc, internalPid, categories, description, jsonSku, productJson)
                  products.add(product)
               }
            } else {
               val name = productJson.optString("skuname")
               val internalId = productJson.optInt("sku", 0).toString()
               val images = scrapImages(doc, productJson, internalPid, internalId)
               val primaryImage: String? = if (!images.isEmpty()) images.get(0) else productJson.optString("image")
               scrapSecondaryImages(images)
               val description = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-description", false)
               val offers = scrapOffers(productJson);


               val product = ProductBuilder()
                  .setUrl(session.originalURL)
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setDescription(description)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setOffers(offers)
                  .build()

               return mutableListOf(product)
               if (product != null) {
                  products.add(product)
               }
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.originalURL)
      }

      return products
   }

   private fun crawlSkuJsonArray(document: Document): JSONArray? {
      val scriptTags = document.getElementsByTag("script")
      var skuJson: JSONObject? = null
      var skuJsonArray: JSONArray? = null
      for (tag: Element in scriptTags) {
         for (node: DataNode in tag.dataNodes()) {
            if (tag.html().trim { it <= ' ' }.startsWith("var skuJson_0 = ")) {
               skuJson = JSONObject(
                  node.wholeData.split(Pattern.quote("var skuJson_0 = ").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                     + node.wholeData.split(Pattern.quote("var skuJson_0 = ").toRegex()).dropLastWhile { it.isEmpty() }
                     .toTypedArray()[1].split(Pattern.quote("}]};").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
               )
            }
         }
      }
      try {
         skuJsonArray = skuJson!!.getJSONArray("skus")
      } catch (e: Exception) {
         e.printStackTrace()
      }
      return skuJsonArray
   }

   private fun scrapOffers(json: JSONObject): Offers {
      val offers = Offers()

      val priceFromString = json.optString("listPriceFormated")
      var priceFrom: Double = MathUtils.parseDoubleWithComma(priceFromString)
      if (priceFrom == 0.00) {
         priceFrom == null
      }

      val stringPrice = json.optString("fullSellingPrice")
      val spotlightPrice: Double = MathUtils.parseDoubleWithComma(stringPrice)
      val bankSlip = spotlightPrice.toBankSlip()
      val sellerName = json.optString("seller")

      val creditCards = listOf(
         Card.MASTERCARD,
         Card.VISA,
         Card.AMEX,
         Card.NATIVA,
         Card.NARANJA,
         Card.CABAL,
         Card.CORDOBESA
      ).toCreditCards(spotlightPrice)

      offers.add(
         OfferBuilder.create()
            .setPricing(
               Pricing.PricingBuilder.create()
                  .setSpotlightPrice(spotlightPrice)
                  .setPriceFrom(priceFrom)
                  .setCreditCards(creditCards)
                  .setBankSlip(bankSlip)
                  .build()
            )
            .setSales(listOf())
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(sellerName)
            .build()
      )

      return offers
   }

}
