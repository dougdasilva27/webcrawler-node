package br.com.lett.crawlernode.crawlers.ratingandreviews.chile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class ChileSalcobrandRatingReviewCrawler extends RatingReviewCrawler {

  public ChileSalcobrandRatingReviewCrawler(Session session) {
    super(session);
  }

  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]",
        "window.chaordic_meta = ", ";", false, false);
    JSONObject productJson = new JSONObject();

    if (json.has("product")) {
      productJson = json.getJSONObject("product");

    }

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);

      if (productJson.has("skus")) {

        for (Object obj : productJson.getJSONArray("skus")) {

          JSONObject sku = (JSONObject) obj;

          String internalId = crawlInternalId(sku);
          Double avgRating = getTotalAvgRating(doc);

          ratingReviews.setInternalId(internalId);
          ratingReviews.setTotalRating(totalNumOfEvaluations);
          ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
          ratingReviews.setAverageOverallRating(avgRating);

        }
      }

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    return ratingReviewsCollection;
  }

  private Double getTotalAvgRating(Document doc) {
    Double totalAvg = null;
    Elements totalAvgElements = doc.select(".write-review .review .selected");

    if (totalAvgElements != null) {
      totalAvg = (double) totalAvgElements.size();
    }

    return totalAvg;
  }

  private Integer getTotalNumOfRatings(Document doc) {
    Element totalRatingsElement = doc.selectFirst(".number-of-reviews");
    Integer totalRatings = null;

    if (totalRatingsElement != null) {
      totalRatings = MathUtils.parseInt(totalRatingsElement.text().trim());
    }

    return totalRatings;
  }

  private String crawlInternalId(JSONObject sku) {
    String id = null;
    JSONArray skus = new JSONArray();

    if (sku.has("sku")) {

      id = sku.get("sku").toString().trim();

    }

    return id;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".big-product-container") != null;
  }

}
