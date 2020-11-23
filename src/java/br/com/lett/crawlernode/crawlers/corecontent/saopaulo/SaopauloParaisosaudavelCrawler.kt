package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrayCommerceCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import org.jsoup.nodes.Document

class SaopauloParaisosaudavelCrawler(session: Session) : TrayCommerceCrawler(session) {
   override fun setSellerName(): String = "Paraíso Saudável"
   override fun getImage(doc: Document?): String? {
      return CrawlerUtils.scrapSimplePrimaryImage(doc, ".image-show .box-img.index-list.active img", listOf("src"),
            "https", ".image-show .box-img.index-list.active")
   }
}
