package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LacoopeencasaCrawler

/**
 * Date: 25/01/21
 *
 * @author Fellype Layunne
 *
 */
class ArgentinaLacoopeencasaCrawler (session: Session) : LacoopeencasaCrawler(session){

   companion object {
      const val LOCAL_ID = br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaLacoopeencasaCrawler.LOCAL_ID
   }

   override fun getLocalId(): String {
      return LOCAL_ID
   }

}
