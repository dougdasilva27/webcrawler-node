package br.com.lett.crawlernode.crawlers.corecontent.campinagrande

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class CampinagrandeRedebellaCrawler(session: Session) : VTEXNewScraper(session) {

   init {
      config.fetcher = FetchMode.JSOUP
   }

   override fun getHomePage(): String {
      return "https://www.redebella.com.br/"
   }

   override fun getMainSellersNames(): MutableList<String> {
      return mutableListOf("NELFARMA COMERCIO DE PRODUTOS QUIMICOS LTDA")
   }

   override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews? {
      return null
   }

}
