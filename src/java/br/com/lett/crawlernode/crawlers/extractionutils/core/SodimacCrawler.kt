package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.htmlOf
import br.com.lett.crawlernode.util.toJson
import com.google.common.collect.Sets
import models.AdvancedRatingReview
import models.Offer.OfferBuilder
import models.Offers
import models.RatingsReviews
import models.pricing.*
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document

open class SodimacCrawler(session: Session?) : Crawler(session) {

   protected var cards: Set<String> = Sets.newHashSet(
      Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString()
   )

   override fun shouldVisit(): Boolean {
      val href = session.originalURL.toLowerCase()
      return !FILTERS.matcher(href).matches() && href.startsWith("https://" + session.options?.optString("homePage") + "/")
   }

   override fun handleCookiesBeforeFetch() {
      session.options?.optJSONObject("cookies")?.toMap()
         ?.forEach { (key: String?, value: Any) -> cookies.add(BasicClientCookie(key, value.toString())) }
   }

   override fun extractInformation(doc: Document): List<Product> {
      super.extractInformation(doc)
      val products: MutableList<Product> = ArrayList()
      if (doc.selectFirst(".product-basic-info") != null) {
         val internalPid = doc.selectFirst(".product-cod").text().split(" ")[1]
         val productName = scrapName(doc)

         val images = scrapImages(internalPid)
         val primaryImage = images.removeFirstOrNull()
         val description = doc.htmlOf(".accordion")
         val categories = scrapCategories(doc)

         val json = CrawlerUtils.selectJsonFromHtml(doc, "script[id=__NEXT_DATA__]", null, null, true, true)
         val variants = JSONUtils.getValueRecursive(json, "props.pageProps.productProps.result.variants", JSONArray::class.java)

         for (variant in variants) {
            variant as JSONObject
            val internalId = variant.optString("id")

            val isAvailable = doc.selectFirst(".out-of-stock-text") == null
            val offers = if (isAvailable) scrapOffers(doc, variant) else Offers()
            val ratings = crawlRatingReviews(internalPid)

            val product: Product = ProductBuilder.create()
               .setUrl(session.originalURL)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(productName)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setRatingReviews(ratings)
               .setOffers(offers)
               .build()

            products.add(product)
         }
      }
      return products
   }

   private fun scrapName(doc: Document): String? {
      val productBrand = CrawlerUtils.scrapStringSimpleInfo(doc, "div.product-brand", true);
      val productModel = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-model", true);
      var productName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-title", false);

      if (productName != null) {
         if (productBrand != null) {
            productName = "$productBrand - $productName"

         }

         if (productModel != null) {
            productName = "$productName - $productModel"
         }
      }

      return productName

   }


   private fun scrapImages(internalPid: String): MutableList<String> {
      val urlImageApi = "https://sodimac.scene7.com/is/image/" + session.options?.optString("region") + "/$internalPid?req=set,json&handler=s7Res&id=imgSet"

      val headers = mapOf(
         "accept" to "*/*",
      )

      val request = Request.RequestBuilder.create()
         .setUrl(urlImageApi)
         .setHeaders(headers)
         .build()

      var apiResponse = dataFetcher.get(session, request)?.body
      apiResponse = apiResponse?.replace(",\"imgSet\");", "")
      apiResponse = apiResponse?.replace("/*jsonp*/s7Res(", "")

      val apiResponseJson = apiResponse?.toJson()
      var imagesJson = JSONArray()
      if (apiResponseJson?.optJSONObject("set")?.opt("item") is JSONArray) {
         imagesJson = apiResponseJson.optJSONObject("set")?.optJSONArray("item")!!
      } else {
         imagesJson.put(apiResponseJson?.optJSONObject("set")?.optJSONObject("item"))
      }

      val imagesUrls = mutableListOf<String>()

      if (imagesJson != null) {
         for (imageObject in imagesJson) {
            if (imageObject is JSONObject) {
               val imageId = imageObject.optJSONObject("s").optString("n")
               if (imageId != null) {
                  imagesUrls.add("https://sodimac.scene7.com/is/image/$imageId?fmt=jpg&fit=fit,1&wid=420&hei=420")
               }
            }
         }
      }

      return imagesUrls
   }

   private fun scrapCategories(doc: Document): CategoryCollection {
      val categories = CrawlerUtils.crawlCategories(doc, "div.bread-crumb-wrapper", false)
      if (!categories.isEmpty()) categories.removeLast()
      return categories
   }

   private fun scrapOffers(doc: Document, json: JSONObject): Offers {
      val offers = Offers()

      val sales = scrapSales(json)

      val pricing: Pricing = scrapPricing(doc)

      offers.add(
         OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(session.options?.optString("sellerFullName"))
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build()
      )

      return offers
   }

   private fun scrapSales(json: JSONObject): List<String> {
      val sales = mutableListOf<String>()

      val badges = json.optJSONObject("badges")

      for (badgeKey in badges.keySet()) {
         val badge = badges.optJSONObject(badgeKey);
         if (badge != null && badge.optString("type") != null && badge.optString("value") != null) {
            sales.add(badge.optString("type") + ": " + badge.optString("value"))
         }
      }

      return sales
   }

   open fun scrapPricing(doc: Document): Pricing {
      var spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".primary", null, false, ',', session)
      var priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".secondary", null, false, ',', session)

      if(spotlightPrice == null){
         spotlightPrice = ScrapSpotlightPrice(doc)
      }
      if(priceFrom == null){
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".m2", null, false, ',', session)
      }

      val bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null)
      val creditCards: CreditCards = scrapCreditCards(spotlightPrice)

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build()
   }

   private fun ScrapSpotlightPrice(doc: Document): Double? {

      var spotlightPrice1 = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price > div > div > span:nth-child(2)", null, false, ',', session)
      var spotlightPrice2 = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price > div > div > span:nth-child(3)", null, false, ',', session)
      if(spotlightPrice1 !== null && spotlightPrice2 !== null){
         return  spotlightPrice1 + spotlightPrice2
      }

      return null;
   }

   protected fun scrapCreditCards(spotlightPrice: Double?): CreditCards {
      val creditCards = CreditCards()
      val installments = Installments()
      if (installments.installments.isEmpty()) {
         installments.add(
            Installment.InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build()
         )
      }
      for (card in cards) {
         creditCards.add(
            CreditCard.CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build()
         )
      }
      return creditCards
   }

   private fun crawlRatingReviews(partnerId: String): RatingsReviews? {
      val ratingReviews = RatingsReviews()

      ratingReviews.date = session.date
      val bazaarVoicePassKey = "catJSr8MNY9Rhy5AbC3aQRhZNLdEJtnLeUvjy4RWAhOSs"
      val endpointRequest = assembleBazaarVoiceEndpointRequest(partnerId, bazaarVoicePassKey, 0, 50)

      val request = Request.RequestBuilder.create()
         .setUrl(endpointRequest)
         .setCookies(cookies)
         .build()

      val ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(dataFetcher[session, request].body)
      val reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, partnerId)
      val advRating = scrapAdvancedRatingReview(reviewStatistics)
      val total = getTotalReviewCount(ratingReviewsEndpointResponse)

      ratingReviews.setTotalRating(total)
      ratingReviews.totalWrittenReviews = total
      ratingReviews.averageOverallRating = getAverageOverallRating(reviewStatistics)
      ratingReviews.advancedRatingReview = advRating

      return ratingReviews
   }

   private fun scrapAdvancedRatingReview(JsonRating: JSONObject): AdvancedRatingReview {
      var star1: Int? = 0
      var star2: Int? = 0
      var star3: Int? = 0
      var star4: Int? = 0
      var star5: Int? = 0
      if (JsonRating.has("RatingDistribution")) {
         val ratingDistribution = JsonRating.optJSONArray("RatingDistribution")
         for (i in 0 until ratingDistribution.length()) {
            val rV = ratingDistribution.optJSONObject(i)
            val val1 = rV.optInt("RatingValue")
            val val2 = rV.optInt("Count")
            when (val1) {
               5 -> star5 = val2
               4 -> star4 = val2
               3 -> star3 = val2
               2 -> star2 = val2
               1 -> star1 = val2
            }
         }
      }
      return AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build()
   }

   private fun getTotalReviewCount(reviewStatistics: JSONObject): Int {
      var totalReviewCount = 0
      val results = JSONUtils.getJSONArrayValue(reviewStatistics, "Results")
      for (result in results) {
         val resultObject = result as JSONObject
         val locale = JSONUtils.getStringValue(resultObject, "ContentLocale")
         if (locale != null && locale.equals("pt_BR", ignoreCase = true)) {
            totalReviewCount++
         }
      }
      return totalReviewCount
   }

   private fun getAverageOverallRating(reviewStatistics: JSONObject): Double? {
      var avgOverallRating = 0.0
      if (reviewStatistics.has("AverageOverallRating")) {
         avgOverallRating = reviewStatistics.optDouble("AverageOverallRating")
      }
      return avgOverallRating
   }

   private fun assembleBazaarVoiceEndpointRequest(skuInternalPid: String, bazaarVoiceEnpointPassKey: String, offset: Int, limit: Int): String {
      val request = StringBuilder()
      request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.5")
      request.append("&passkey=$bazaarVoiceEnpointPassKey")
      request.append("&Offset=$offset")
      request.append("&Limit=$limit")
      request.append("&Sort=SubmissionTime:desc")
      request.append("&Filter=ProductId:$skuInternalPid")
      request.append("&Include=Products")
      request.append("&Stats=Reviews")
      return request.toString()
   }


   private fun getReviewStatisticsJSON(ratingReviewsEndpointResponse: JSONObject, skuInternalPid: String): JSONObject {
      if (ratingReviewsEndpointResponse.has("Includes")) {
         val includes = ratingReviewsEndpointResponse.optJSONObject("Includes")
         if (includes.has("Products")) {
            val products = includes.optJSONObject("Products")
            if (products.has(skuInternalPid)) {
               val product = products.optJSONObject(skuInternalPid)
               if (product.has("ReviewStatistics")) {
                  return product.optJSONObject("ReviewStatistics")
               }
            }
         }
      }
      return JSONObject()
   }
}
