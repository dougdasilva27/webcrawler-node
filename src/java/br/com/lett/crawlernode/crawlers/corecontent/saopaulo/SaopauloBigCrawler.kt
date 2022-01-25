package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.regex.Pattern

class SaopauloBigCrawler(session: Session) : VTEXOldScraper(session) {
   override fun handleCookiesBeforeFetch() {
      cookies.add(BasicClientCookie("vtex_segment", session.options.optString("vtex_segment")))
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
      if(sellerName != null && sellerName.last() == '.') {
         seller = sellerName.substring(0, sellerName.length - 1)
      }
      return super.isMainRetailer(seller)
   }

   override fun scrapPidFromApi(doc: Document?): String? {
      var internalPid: String? = null
      val pattern = Pattern.compile("id\":\"(.[0-9]*)\",\"slug")
      val matcher = pattern.matcher(doc.toString())
      if (matcher.find()) {
         internalPid = matcher.group(1)

      }

      return internalPid
   }

}
