package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilSemardriveCrawler

/**
 * Date: 22/10/20
 *
 * @author Fellype Layunne
 *
 */

class SaopauloSemardrivearujaCrawler(session: Session) : BrasilSemardriveCrawler(session) {

   companion object {
      const val ZIP_CODE: String = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloSemardrivearujaCrawler.ZIP_CODE
   }

   override fun getZipCode(): String {
      return ZIP_CODE
   }
}
