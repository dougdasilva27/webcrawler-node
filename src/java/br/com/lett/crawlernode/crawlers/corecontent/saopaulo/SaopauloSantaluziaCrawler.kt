package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper
import br.com.lett.crawlernode.util.JSONUtils
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class SaopauloSantaluziaCrawler(session: Session) : VTEXOldScraper(session) {

    companion object {
        private const val HOME_PAGE = "https://www.santaluzia.com.br/"
        private val MAIN_SELLERS = mutableListOf("Casa Santa Luzia", "santaluzia")
    }

    override fun getHomePage(): String {
        return HOME_PAGE
    }

    override fun getMainSellersNames(): MutableList<String> {
        return MAIN_SELLERS
    }

    override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews {
        return RatingsReviews()
    }

    override fun scrapDescription(doc: Document, productJson: JSONObject): String {
        val description = StringBuilder()

        if (productJson.optString("description").isNotEmpty()) {
            description.append(JSONUtils.getStringValue(productJson, "description"))
        }

        description.append(extractInfoFromJsonArray(productJson, "Informações"))
        description.append(extractInfoFromJsonArray(productJson, "Características"))
        description.append(extractInfoFromJsonArray(productJson, "Ingredientes"))
        description.append(extractInfoFromJsonArray(productJson, "Tabela Nutricional"))
        description.append(extractInfoFromJsonArray(productJson, "Curiosidades"))

        return description.toString()
    }

    private fun extractInfoFromJsonArray(product: JSONObject, key: String) : String {
        if (product.optJSONArray(key) != null) {
            return JSONUtils.getJSONArrayValue(product, key).optString(0)
        }
        return ""
    }
}