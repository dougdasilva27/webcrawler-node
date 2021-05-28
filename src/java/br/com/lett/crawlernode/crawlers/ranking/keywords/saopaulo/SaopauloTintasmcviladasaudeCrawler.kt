package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloTintasmcviladasaudeCrawler
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TintasmcRanking

class SaopauloTintasmcviladasaudeCrawler(session: Session) : TintasmcRanking(session) {

   override fun setJsonLocation(): String = SaopauloTintasmcviladasaudeCrawler.locationJson
}
