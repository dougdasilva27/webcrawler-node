package br.com.lett.crawlernode.crawlers.corecontent.bracodonorte

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.TieliSupermercadoCrawler

class BracodonorteTielisupermercadoCrawler(session: Session) : TieliSupermercadoCrawler(session) {
    override val emp: String
        get() = "1"
}