package br.com.lett.crawlernode.crawlers.extractionutils.core

import br.com.lett.crawlernode.core.session.Session
import models.RatingsReviews
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.nodes.Document

class VTEXNewImpl(session: Session) : VTEXNewScraper(session) {


   override fun handleCookiesBeforeFetch() {
      session.options?.optJSONObject("cookies")?.toMap()
         ?.forEach { (key: String?, value: Any) ->
            val cookie = BasicClientCookie(key, value.toString())

            if(key == "vtex_segment")
               cookie.domain = if(getHomePage().endsWith("/")) getHomePage().substringAfter("https://").replace("/", "") else getHomePage().substringAfter("https://")

            cookies.add(cookie)
         }
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
}
