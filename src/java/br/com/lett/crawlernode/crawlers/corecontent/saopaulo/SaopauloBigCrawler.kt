package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.util.CrawlerUtils
import models.RatingsReviews
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.nodes.Document

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


   override fun scrapInternalpid(doc: Document?): String? {
      val productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, "}", true, true)
      var internalPid: String? = null
      if (productJson.has("mpn")) {
         internalPid = productJson.optString("mpn")
      }
      return internalPid
   }
}
