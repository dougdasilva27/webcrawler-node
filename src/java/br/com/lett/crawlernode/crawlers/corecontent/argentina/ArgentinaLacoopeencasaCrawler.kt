package br.com.lett.crawlernode.crawlers.corecontent.argentina

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.LacoopeencasaCrawler

/**
 * Date: 25/01/21
 *
 * @author Fellype Layunne
 *
 */
class ArgentinaLacoopeencasaCrawler (session: Session) : LacoopeencasaCrawler(session){

   companion object {
      const val LOCAL_ID: String = "93"
   }

   override fun getLocalId(): String {
      return LOCAL_ID
   }

}
