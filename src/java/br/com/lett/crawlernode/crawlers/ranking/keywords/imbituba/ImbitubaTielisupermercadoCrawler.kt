package br.com.lett.crawlernode.crawlers.ranking.keywords.imbituba

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TieliSupermercadoRanking

class ImbitubaTielisupermercadoCrawler(session: Session) : TieliSupermercadoRanking(session) {
    override val emp: String
        get() = "4"
}