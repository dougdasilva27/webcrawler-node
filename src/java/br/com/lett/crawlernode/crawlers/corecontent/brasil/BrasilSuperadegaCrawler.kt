package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.YourreviewsRatingCrawler
import models.AdvancedRatingReview
import models.RatingsReviews
import org.json.JSONObject
import org.jsoup.nodes.Document

class BrasilSuperadegaCrawler(session: Session) : VTEXOldScraper(session) {

    override fun getHomePage(): String {
        return "https://www.superadega.com.br/"
    }

    override fun getMainSellersNames(): MutableList<String> {
        return mutableListOf("Super Adega")
    }

    override fun scrapRating(internalId: String, internalPid: String, doc: Document, jsonSku: JSONObject): RatingsReviews {
        val ratingReviews = RatingsReviews()
        ratingReviews.date = session.date

        val yr = YourreviewsRatingCrawler(session, cookies, Crawler.logger, "8efd852a-d220-49a7-9bd8-e84c2b09b66a", dataFetcher)
        val docRating = yr.crawlPageRatingsFromYourViews(internalId, "8efd852a-d220-49a7-9bd8-e84c2b09b66a", dataFetcher)

        val totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating)
        val avgRating = getTotalAvgRatingFromYourViews(docRating)
        val advancedRatingReview = getTotalStarsFromEachValue(internalId, yr)

        ratingReviews.advancedRatingReview = advancedRatingReview
        ratingReviews.setTotalRating(totalNumOfEvaluations)
        ratingReviews.averageOverallRating = avgRating
        ratingReviews.totalWrittenReviews = totalNumOfEvaluations
        return ratingReviews
    }

    fun getTotalNumOfRatingsFromYourViews(doc: Document): Int {
        var totalRating = 0
        val totalRatingElement = doc.select("strong[itemprop=count]").first()
        if (totalRatingElement != null) {
            val totalText = totalRatingElement.ownText().replace("[^0-9]".toRegex(), "").trim { it <= ' ' }
            if (totalText.isNotEmpty()) {
                totalRating = totalText.toInt()
            }
        }
        return totalRating
    }

    fun getTotalAvgRatingFromYourViews(docRating: Document): Double {
        var avgRating = 0.0
        val rating = docRating.selectFirst("meta[itemprop=rating]")
        if (rating != null) {
            avgRating = rating.attr("content").toDouble()
        }
        return avgRating
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
            docRating = yr.crawlAllPagesRatingsFromYourViews(internalPid, "8efd852a-d220-49a7-9bd8-e84c2b09b66a", dataFetcher, currentPage)
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
        return AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build()
    }

    private fun hasNextPage(docRating: Document, currentPage: Int): Boolean {
        val pages = docRating.select(".yv-paging:not(:last-child)")
        return !pages.isEmpty() && pages[pages.size - 1].text().trim() != currentPage.toString()
    }
}