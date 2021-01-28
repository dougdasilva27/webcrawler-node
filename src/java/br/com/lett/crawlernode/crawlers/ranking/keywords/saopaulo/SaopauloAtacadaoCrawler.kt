package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.AtacadaoCrawlerRanking

/**
 * Date: 28/01/21
 *
 * @author Fellype Layunne
 *
 */
class SaopauloAtacadaoCrawler (session: Session) : AtacadaoCrawlerRanking(session){

   companion object {
      const val CITY_ID: String = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloAtacadaoCrawler.CITY_ID
   }

   override fun getCityId(): String {
      return CITY_ID
   }

}
