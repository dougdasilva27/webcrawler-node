package br.com.lett.crawlernode.crawlers.corecontent.campinas

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.crawlers.extractionutils.core.VtexRatingCrawler
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.util.*

class CampinasPetcampCrawler(session: Session) : VTEXOldScraper(session) {

   companion object {
      val HOME_PAGE = "https://www.petcamp.com.br/";
      val SELLER_FULL_NAME = Arrays.asList("pc paulinia comercio de produtos agropecuarios ltda.");
   }

   override fun getHomePage(): String {
      return HOME_PAGE;
   }

   override fun getMainSellersNames(): MutableList<String> {
      return SELLER_FULL_NAME;
   }

   override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews {
      return VtexRatingCrawler(session, HOME_PAGE, logger, cookies)
         .extractRatingAndReviews(internalId, doc, dataFetcher)
   }


}
