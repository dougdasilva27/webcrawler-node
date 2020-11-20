package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler
import br.com.lett.crawlernode.util.CrawlerUtils
import models.AdvancedRatingReview
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilSuperadegaCrawler(session: Session) : VTEXOldScraper(session) {

    private val STORE_KEY = "8efd852a-d220-49a7-9bd8-e84c2b09b66a"

    override fun getHomePage(): String {
        return "https://www.superadega.com.br/"
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf("Super Adega")
    }

    override fun scrapDescription(doc: Document?, productJson: JSONObject?): String {
        return CrawlerUtils.scrapElementsDescription(doc, listOf(".product-description-specs-content"))
    }

    override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews {
        val ratingReviews = RatingsReviews()
        ratingReviews.date = session.date

        val yr = YourreviewsRatingCrawler(session, cookies, Crawler.logger, STORE_KEY, dataFetcher)
        val docRating = yr.crawlPageRatingsFromYourViews(internalId, STORE_KEY, dataFetcher)

        val totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating)

        ratingReviews.advancedRatingReview = getTotalStarsFromEachValue(internalId, yr)
        ratingReviews.averageOverallRating = getTotalAvgRatingFromYourViews(docRating)
        ratingReviews.totalWrittenReviews = totalNumOfEvaluations
        ratingReviews.setTotalRating(totalNumOfEvaluations)
        return ratingReviews
    }

    fun getTotalNumOfRatingsFromYourViews(doc: Document): Int {
        val totalRatingElement = doc.select("strong[itemprop=count]").first()
        if (totalRatingElement != null) {
            val totalText = totalRatingElement.ownText().replace("[^0-9]".toRegex(), "").trim()
            return if (totalText.isNotEmpty()) totalText.toInt() else 0
        }
        return 0
    }

    fun getTotalAvgRatingFromYourViews(docRating: Document): Double {
        val rating = docRating.selectFirst("meta[itemprop=rating]")
        return rating?.attr("content")?.toDouble() ?: 0.0
    }

    fun getTotalStarsFromEachValue(internalPid: String, yr: YourreviewsRatingCrawler): AdvancedRatingReview {
        var docRating: Document
        var currentPage = 0
        var star1 = 0
        var star2 = 0
        var star3 = 0
        var star4 = 0
        var star5 = 0

        do {
            currentPage++
            docRating = yr.crawlAllPagesRatingsFromYourViews(internalPid, STORE_KEY, dataFetcher, currentPage)
            val reviews = docRating.select(".yv-col-md-8")
            for (element in reviews) {
                val stars = element.select(".fa-star")
                if (stars.size == 1) {
                    star1++
                }
                if (stars.size == 2) {
                    star2++
                }
                if (stars.size == 3) {
                    star3++
                }
                if (stars.size == 4) {
                    star4++
                }
                if (stars.size == 5) {
                    star5++
                }
            }
        } while (hasNextPage(docRating, currentPage))

        return AdvancedRatingReview.Builder()
                .totalStar1(star1)
                .totalStar2(star2)
                .totalStar3(star3)
                .totalStar4(star4)
                .totalStar5(star5)
                .build()
    }

    private fun hasNextPage(docRating: Document, currentPage: Int): Boolean {
        val pages = docRating.select(".yv-paging:not(:last-child)")
        return !pages.isEmpty() && pages[pages.size - 1].text().trim() != currentPage.toString()
    }
}