package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilTintasmcCrawler
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TintasmcRanking

class BrasilTintasmcCrawler(session: Session) : TintasmcRanking(session) {

   override fun setJsonLocation(): String = BrasilTintasmcCrawler.locationJson
}
