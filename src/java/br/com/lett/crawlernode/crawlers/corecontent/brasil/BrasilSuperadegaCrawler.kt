package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import models.AdvancedRatingReview
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilSuperadegaCrawler(session: Session) : VTEXOldScraper(session) {

   private val STORE_KEY = "8efd852a-d220-49a7-9bd8-e84c2b09b66a"
   private var starsSum = 0;
   private var starsCount = 0;
   override fun getHomePage(): String {
      return "https://www.superadega.com.br/"
   }

   override fun getMainSellersNames(): MutableList<String> {
      return mutableListOf("Super Adega")
   }

   override fun scrapDescription(doc: Document?, productJson: JSONObject?): String {
      return CrawlerUtils.scrapElementsDescription(doc, listOf(".product-description-specs-content"))
   }
   
  private fun hasNextPage(docRating: Document, currentPage: Int): Boolean {
      val pages = docRating.select(".yv-paging:not(:last-child)")
      return !pages.isEmpty() && pages[pages.size - 1].text().trim() != currentPage.toString()
   }
}
