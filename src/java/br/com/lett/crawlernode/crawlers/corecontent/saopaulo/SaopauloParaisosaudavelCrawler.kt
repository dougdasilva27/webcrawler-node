package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrayCommerceCrawler

class SaopauloParaisosaudavelCrawler(session: Session) : TrayCommerceCrawler(session) {
    override fun setSellerName(): String = "Paraíso Saudável"
}
