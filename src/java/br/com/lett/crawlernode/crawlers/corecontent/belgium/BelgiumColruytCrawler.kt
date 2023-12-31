package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.session.Session

/**
 * Date: 23/10/20
 *
 * @author Fellype Layunne
 *
 */

// placeId default do site
class BelgiumColruytCrawler(session: Session) : ColruytCrawler(session) {

   companion object {
      const val PLACE_ID = "604"
   }

   override fun getPlaceId(): String {
      return PLACE_ID
   }

}
