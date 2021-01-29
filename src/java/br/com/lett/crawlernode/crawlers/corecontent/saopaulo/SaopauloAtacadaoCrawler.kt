package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.AtacadaoCrawler

/**
 * Date: 27/01/21
 *
 * @author Fellype Layunne
 *
 */
class SaopauloAtacadaoCrawler (session: Session) : AtacadaoCrawler(session){

   companion object {
      const val CITY_ID: String = "5270"
   }

   override fun getCityId(): String {
      return CITY_ID
   }

}
