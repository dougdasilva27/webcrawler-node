package br.com.lett.crawlernode.crawlers.corecontent.peru

import br.com.lett.crawlernode.core.models.Card
import kotlin.Throws
import java.lang.Exception
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.core.models.CategoryCollection
import br.com.lett.crawlernode.core.models.Product
import models.prices.Prices
import models.Marketplace
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.MathUtils
import models.RatingsReviews
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.net.URL
import java.util.*

class PeruPlazaveaCrawler(session: Session?) : VTEXOldScraper(session) {

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

}
