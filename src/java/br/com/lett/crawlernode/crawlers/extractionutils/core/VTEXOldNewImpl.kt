package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.JSONUtils
import models.RatingsReviews
import models.pricing.Pricing
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.io.UnsupportedEncodingException

class VTEXOldNewImpl(session: Session) : VTEXOldScraper(session) {
   override fun handleCookiesBeforeFetch() {
      session.options?.optJSONObject("cookies")?.toMap()
         ?.forEach { (key: String?, value: Any) -> cookies.add(BasicClientCookie(key, value.toString())) }
   }

   override fun getHomePage(): String {
      return session.options.optString("homePage")
   }

   override fun getMainSellersNames(): List<String> {
      return session.options?.optJSONArray("sellers")?.toList()
         ?.map { obj: Any -> obj.toString() } ?: listOf()
   }

   override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews? {
      return null
   }

   override fun scrapPricing(doc: Document?, internalId: String?, comertial: JSONObject?, discountsJson: JSONObject?): Pricing {
      val pricing = super.scrapPricing(doc, internalId, comertial, discountsJson);

      if (session.options.optBoolean("useOnlySpotPrice", false)) {
         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(pricing.spotlightPrice)
            .setPriceFrom(pricing.priceFrom)
            .setBankSlip(CrawlerUtils.setBankSlipOffers(pricing.spotlightPrice, null))
            .build()
      }

      return pricing;
   }

   @Throws(UnsupportedEncodingException::class)
   override fun scrapDescription(doc: Document?, productJson: JSONObject?): String? {
      val description = JSONUtils.getStringValue(productJson, "description")

      if (description.isNullOrEmpty() && !JSONUtils.getJSONArrayValue(productJson, "Vinhos Especiais").isEmpty) {
         val descriptionKeys = JSONUtils.getJSONArrayValue(productJson, "Vinhos Especiais");

         if (!descriptionKeys.isEmpty) {
            var wineDescription = arrayOf<String>()

            for (item in descriptionKeys) {
               var keyValue = productJson?.getJSONArray(item.toString())
               wineDescription = wineDescription.plus(item.toString() + ": " + keyValue?.getString(0))
            }
            return wineDescription.joinToString(",")
         }
      }
      return description
   }
}
