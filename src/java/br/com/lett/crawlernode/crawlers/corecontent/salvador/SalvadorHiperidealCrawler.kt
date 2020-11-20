package br.com.lett.crawlernode.crawlers.corecontent.salvador

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class SalvadorHiperidealCrawler(session: Session) : VTEXOldScraper(session) {

  override fun getHomePage(): String {
    return "https://www.hiperideal.com.br/"
  }

  override fun getMainSellersNames(): MutableList<String> {
    return mutableListOf("Hiper Ideal")
  }

  override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews {
    return RatingsReviews()
  }
}