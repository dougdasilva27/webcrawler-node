package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper
import br.com.lett.crawlernode.util.JSONUtils
import br.com.lett.crawlernode.util.MathUtils
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class BrasilNagumoCrawler(session: Session) : VTEXOldScraper(session) {

    override fun getHomePage(): String {
        return "https://www.nagumo.com.br/"
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf("Supermercados Nagumo")
    }

    override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews {
        val ratingReviews = RatingsReviews()
        ratingReviews.date = session.date

        val docRating = crawlPageRatings(session.originalURL, internalPid)

        val totalNumOfEvaluations: Int = getTotalNumOfRatings(docRating)
        val avgRating: Double = getTotalAvgRating(docRating, totalNumOfEvaluations)

        ratingReviews.setTotalRating(getTotalNumOfReviews(docRating))
        ratingReviews.averageOverallRating = avgRating
        return ratingReviews
    }

    private fun crawlPageRatings(url: String, internalPid: String): Document {
        var doc = Document(url)

        val tokens = url.split("/".toRegex()).toTypedArray()
        val productLinkId = tokens[tokens.size - 2]
        val payload = "productId=$internalPid&productLinkId=$productLinkId"

        val headers: MutableMap<String, String> = HashMap()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        headers["Accept-Language"] = "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4"

        val request = Request.RequestBuilder.create().setUrl("https://www.nagumo.com.br/userreview").setCookies(cookies).setHeaders(headers)
                .setPayload(payload).build()
        val response = dataFetcher.post(session, request).body
        if (response != null) {
            doc = Jsoup.parse(response)
        }
        return doc
    }

    private fun getTotalAvgRating(docRating: Document, totalRating: Int): Double {
        var avgRating = 0.0
        val rating = docRating.select(".rating > li")
        if (totalRating > 0) {
            var total = 0.0
            for (e in rating) {
                val star = e.select(".voteRatingStar").first()
                val totalStar = e.select(".voteRatingBar").first()
                if (totalStar != null) {
                    val votes = totalStar.text().replace("[^0-9]".toRegex(), "").trim { it <= ' ' }
                    if (votes.isNotEmpty()) {
                        val totalVotes = votes.toInt()
                        if (star != null) {
                            if (star.hasClass("avaliacao50")) {
                                total += totalVotes * 5.toDouble()
                            } else if (star.hasClass("avaliacao40")) {
                                total += totalVotes * 4.toDouble()
                            } else if (star.hasClass("avaliacao30")) {
                                total += totalVotes * 3.toDouble()
                            } else if (star.hasClass("avaliacao20")) {
                                total += totalVotes * 2.toDouble()
                            } else if (star.hasClass("avaliacao10")) {
                                total += totalVotes * 1.toDouble()
                            }
                        }
                    }
                }
            }
            avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalRating)
        }
        return avgRating
    }

    private fun getTotalNumOfRatings(docRating: Document): Int {
        var totalRating = 0
        val totalRatingElement = docRating.select("#spnRatingProdutoBottom").first()
        if (totalRatingElement != null) {
            val totalText = totalRatingElement.ownText().replace("[^0-9]".toRegex(), "").trim { it <= ' ' }
            if (totalText.isNotEmpty()) {
                totalRating = totalText.toInt()
            }
        }
        return totalRating
    }

    private fun getTotalNumOfReviews(docRating: Document): Int {
        return docRating.select(".resenhas .quem > li").size
    }

    override fun scrapDescription(doc: Document, productJson: JSONObject): String {
        val description = StringBuilder()

        if (productJson.optString("description").isNotEmpty()) {
            description.append(JSONUtils.getStringValue(productJson, "description"))
        }

        description.append(extractInfoFromJsonArray(productJson, "Informações"))
        description.append(extractInfoFromJsonArray(productJson, "Características"))
        description.append(extractInfoFromJsonArray(productJson, "Tabela Nutricional"))
        description.append(extractInfoFromJsonArray(productJson, "Ingredientes"))
        description.append(extractInfoFromJsonArray(productJson, "Curiosidades"))

        return description.toString()
    }

    private fun extractInfoFromJsonArray(product: JSONObject, key: String): String {
        if (product.optJSONArray(key) != null) {
            return JSONUtils.getJSONArrayValue(product, key).optString(0)
        }
        return ""
    }
}