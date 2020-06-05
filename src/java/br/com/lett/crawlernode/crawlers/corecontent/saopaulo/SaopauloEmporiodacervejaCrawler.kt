package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class SaopauloEmporiodacervejaCrawler(session: Session) : VTEXNewScraper(session) {

    private val HOME_PAGE = "https://www.emporiodacerveja.com.br/"
    private val MAIN_SELLER_NAME_LOWER = "emporiodacerveja.com.br"

    override fun getHomePage(): String {
        return HOME_PAGE
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf(MAIN_SELLER_NAME_LOWER)
    }

    override fun scrapRating(internalId: String?, internalPid: String?, doc: Document?, jsonSku: JSONObject?): RatingsReviews? {
        return null
    }
}