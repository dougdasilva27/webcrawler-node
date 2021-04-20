package br.com.lett.crawlernode.crawlers.corecontent.laguna

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.TieliSupermercadoCrawler

class LagunaTielisupermercadoCrawler(session: Session) : TieliSupermercadoCrawler(session) {
    override val emp: String
        get() = "3"
}