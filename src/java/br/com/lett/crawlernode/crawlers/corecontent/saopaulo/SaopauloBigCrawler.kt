package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.regex.Pattern

class SaopauloBigCrawler(session: Session) : VTEXNewScraper(session) {
   override fun handleCookiesBeforeFetch() {
      val cookie = BasicClientCookie("vtex_segment", session.options.optString("vtex_segment"))
      cookie.domain = if (getHomePage().endsWith("/")) getHomePage().substringAfter("https://").replace("/", "") else getHomePage().substringAfter("https://")
      cookies.add(cookie)
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

   override fun isMainRetailer(sellerName: String?): Boolean {
      var seller = sellerName ?: ""
      if (sellerName != null && sellerName.last() == '.') {
         seller = sellerName.substring(0, sellerName.length - 1)
      }
      return super.isMainRetailer(seller)
   }

   override fun scrapDescription(doc: Document?, productJson: JSONObject?): String {
      val builder = StringBuilder();
      builder.append(productJson?.optString("description")).append("</br></br>Informações do produto</br></br>")

      val keys = productJson?.optJSONArray("allSpecifications")?.toList();

      if (keys != null) {
         for (key in keys) {
            val keyName = key.toString()
            val keyValue = productJson.optJSONArray(keyName)?.toList()?.first()?.toString()
            if (keyValue != null) {
               builder.append(keyName).append(": ").append(keyValue).append("</br>")
            }
         }
      }
      return builder.toString()
   }
}

