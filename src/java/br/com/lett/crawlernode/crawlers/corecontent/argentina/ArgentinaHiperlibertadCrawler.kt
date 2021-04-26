package br.com.lett.crawlernode.crawlers.corecontent.argentina

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.apache.http.impl.cookie.BasicClientCookie
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ArgentinaHiperlibertadCrawler(session: Session) : VTEXOldScraper(session) {

   override fun getHomePage(): String {
      return "https://www.hiperlibertad.com.ar/"
   }

   override fun fetch(): Any {
      cookies.add(BasicClientCookie("VTEXSC", "sc=1"))
      val request = RequestBuilder.create().setUrl(session.originalURL).setCookies(cookies).build()
      return Jsoup.parse(dataFetcher[session, request].body)
   }

   override fun getMainSellersNames(): MutableList<String> {
      return mutableListOf("Hiper Liberdade", "LIBERTAD SA")
   }

   override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews? {
      return null
   }
}
