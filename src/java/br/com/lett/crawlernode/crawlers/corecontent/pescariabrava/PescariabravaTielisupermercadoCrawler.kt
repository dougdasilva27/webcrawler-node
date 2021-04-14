package br.com.lett.crawlernode.crawlers.corecontent.pescariabrava

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.TieliSupermercadoCrawler

class PescariabravaTielisupermercadoCrawler(session: Session) : TieliSupermercadoCrawler(session) {
    override val emp: String
        get() = "2"
}