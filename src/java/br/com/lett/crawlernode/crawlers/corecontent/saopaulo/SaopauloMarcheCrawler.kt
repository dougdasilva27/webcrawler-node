package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.MarcheCrawler
import br.com.lett.crawlernode.util.*

class SaopauloMarcheCrawler(session: Session) : MarcheCrawler(session) {

   companion object {
      const val CEP = "05303000"
      const val SELLER_NAME = "marche sao paulo"
   }

   override fun getCEP(): String {
      return CEP
   }

   override fun getSellerName(): String {
      return SELLER_NAME
   }
}
