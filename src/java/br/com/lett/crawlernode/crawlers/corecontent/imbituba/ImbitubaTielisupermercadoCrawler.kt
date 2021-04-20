package br.com.lett.crawlernode.crawlers.corecontent.imbituba

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.TieliSupermercadoCrawler

class ImbitubaTielisupermercadoCrawler(session: Session) : TieliSupermercadoCrawler(session) {
    override val emp: String
        get() = "4"
}