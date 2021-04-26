package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.int
import org.jsoup.nodes.Element

class BrasilSanmichelCrawler(session: Session) : BrasilSitemercadoCrawler(session) {


   override fun getHomePage(): String {
      return "https://www.sitemercado.com.br/sanmichelsupermercados/pocos-de-caldas-loja-pernambuco-centro-r-pernambuco"
   }


}
