package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.TrayCommerceCrawler


class SaopauloParaisosaudavelCrawler(session: Session) : TrayCommerceCrawler(session) {

  override fun setStoreId(): String = "738124"

  override fun setHomePage(): String = "https://www.paraisosaudavel.com.br/"
}
