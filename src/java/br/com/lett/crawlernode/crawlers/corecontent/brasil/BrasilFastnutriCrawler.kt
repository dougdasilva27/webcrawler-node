package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilFastnutriCrawler(session: Session) : VTEXOldScraper(session) {

  override fun getHomePage(): String = "https://www.fastnutri.com.br/"

  override fun getMainSellersNames(): MutableList<String> {
    return mutableListOf("Fast Nutri")
  }

  override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews {
    return RatingsReviews()
  }

}