package br.com.lett.crawlernode.crawlers.corecontent.itaitinga

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.MerconnectCrawler

class ItaitingaIsmaelsupermercadoCrawler(session: Session) : MerconnectCrawler(session) {

   override fun getStoreId() = "71"
   override fun getSellerName() = "Ismael Supermercado"
   override fun getClientId() = "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff"
   override fun getClientSecret() = "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c"
}

