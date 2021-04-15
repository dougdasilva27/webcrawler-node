package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.*
import org.jsoup.nodes.Document
import java.util.HashMap

class BrasilSanmichelCrawler(session: Session) : BrasilSitemercadoCrawler(session) {
   
   companion object {
      val HOME_PAGE = "https://www.sitemercado.com.br/sanmichelsupermercados/pocos-de-caldas-loja-pernambuco-centro-r-pernambuco"

      val IDLOJA = 6755
      val IDREDE = 3748
   }


   override fun getHomePage(): String {
      return HOME_PAGE
   }

   override fun getLojaInfo(): Map<String, Int> {
      val lojaInfo: MutableMap<String, Int> = HashMap()
      lojaInfo["IdLoja"] = IDLOJA
      lojaInfo["IdRede"] = IDREDE
      return lojaInfo
   }
}
