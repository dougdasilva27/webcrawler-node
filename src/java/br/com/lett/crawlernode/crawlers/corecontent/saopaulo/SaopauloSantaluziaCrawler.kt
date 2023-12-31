package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.util.JSONUtils
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class SaopauloSantaluziaCrawler(session: Session) : VTEXOldScraper(session) {

    override fun getHomePage(): String {
        return "https://www.santaluzia.com.br/"
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf("Casa Santa Luzia", "santaluzia")
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