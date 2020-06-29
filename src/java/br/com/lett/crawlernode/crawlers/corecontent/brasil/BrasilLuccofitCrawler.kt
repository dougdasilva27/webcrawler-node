package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper
import br.com.lett.crawlernode.util.CrawlerUtils
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilLuccofitCrawler(session: Session) : VTEXOldScraper(session) {

    override fun getHomePage(): String {
        return "https://www.luccofit.com.br/"
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf("Lucco Fit Refeições LTDA. EPP")
    }

    override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews {
        return RatingsReviews()
    }

    override fun scrapDescription(doc: Document, productJson: JSONObject): String {
        return CrawlerUtils.scrapSimpleDescription(doc, mutableListOf("#description", "#specification", ".banner-aquecer-mobile"))
    }
}