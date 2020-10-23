package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.session.Session

/**
 * Date: 21/10/20
 *
 * @author Fellype Layunne
 *
 */

// placeId default do site
class BelgiumColruythalleCrawler(session: Session) : BelgiumColruytCrawler(session) {

   companion object {
      const val PLACE_ID = "604"
   }

   override fun getPlaceId(): String {
      return PLACE_ID
   }

}
