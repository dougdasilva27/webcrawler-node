package br.com.lett.crawlernode.crawlers.corecontent.peru

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class PeruPlazaveaCrawler(session: Session) : VTEXOldScraper(session) {

   companion object {
      private val HOME_PAGE = "https://www.plazavea.com.pe/"
      private val MAIN_SELLER_NAME_LOWER = "plaza vea"
   }

   override fun getHomePage(): String {
      return HOME_PAGE
   }

   override fun getMainSellersNames(): MutableList<String> {
      return mutableListOf(MAIN_SELLER_NAME_LOWER)
   }

   override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews? {
      return null
   }

   override fun scrapDescription(doc: Document?, productJson: JSONObject): String? {
      return productJson.optString("Descripción del producto")
   }
}
