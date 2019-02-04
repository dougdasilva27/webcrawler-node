package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class BrasilCasaeconstrucaoRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilCasaeconstrucaoRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalId = crawlInternalId(document);

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "53f7df57-db2b-4521-b829-617abf75405");
      Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-identification").isEmpty();
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(document, "script", "var google_tag_params = ", ";", false, false);
    if (skuJson.has("ecomm_prodid")) {
      internalId = skuJson.getString("ecomm_prodid");
    }

    return internalId;
  }
}

