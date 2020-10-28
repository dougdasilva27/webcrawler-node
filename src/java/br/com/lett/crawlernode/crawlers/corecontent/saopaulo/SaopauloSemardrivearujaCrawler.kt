package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilSemardriveCrawler

/**
 * Date: 22/10/20
 *
 * @author Fellype Layunne
 *
 */

class SaopauloSemardrivearujaCrawler(session: Session) : BrasilSemardriveCrawler(session) {

   companion object {
      const val ZIP_CODE: String = "07401-070"
   }

   override fun getZipCode(): String {
      return ZIP_CODE
   }
}
