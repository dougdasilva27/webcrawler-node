package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
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

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "53f7df57-db2b-4521-b829-617abf75405d", dataFetcher);

      Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating, "p[itemprop=\"count\"]");

      Double avgRating = getTotalAvgRatingFromYourViews(docRating, ".rating-number .yv-count-stars1");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }



  private Double getTotalAvgRatingFromYourViews(Document docRating, String cssSelector) {
    Double avgRating = 0d;
    Element rating = docRating.select(cssSelector).first();

    if (rating != null) {
      avgRating = MathUtils.parseDoubleWithDot(rating.text().trim());
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatingsFromYourViews(Document doc, String cssSelector) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.select(cssSelector).first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
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

