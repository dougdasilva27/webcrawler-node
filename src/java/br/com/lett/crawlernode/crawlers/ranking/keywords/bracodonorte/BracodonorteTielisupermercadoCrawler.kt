package br.com.lett.crawlernode.crawlers.ranking.keywords.bracodonorte

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TieliSupermercadoRanking

class BracodonorteTielisupermercadoCrawler(session: Session) : TieliSupermercadoRanking(session) {
    override val emp: String
        get() = "1"
}