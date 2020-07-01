package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class SaopauloEmporiodacervejaCrawler(session: Session) : VTEXNewScraper(session) {

    override fun getHomePage(): String {
        return "https://www.emporiodacerveja.com.br/"
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf("emporiodacerveja.com.br")
    }

    override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews? {
        return null
    }
}