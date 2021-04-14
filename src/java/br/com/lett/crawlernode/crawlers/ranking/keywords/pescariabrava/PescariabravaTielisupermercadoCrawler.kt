package br.com.lett.crawlernode.crawlers.ranking.keywords.pescariabrava

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TieliSupermercadoRanking

class PescariabravaTielisupermercadoCrawler(session: Session) : TieliSupermercadoRanking(session) {
    override val emp: String
        get() = "2"
}