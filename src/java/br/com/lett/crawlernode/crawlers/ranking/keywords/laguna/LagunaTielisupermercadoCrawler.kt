package br.com.lett.crawlernode.crawlers.ranking.keywords.laguna

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TieliSupermercadoRanking

class LagunaTielisupermercadoCrawler(session: Session) : TieliSupermercadoRanking(session) {
    override val emp: String
        get() = "3"
}