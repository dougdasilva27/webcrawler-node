package br.com.lett.crawlernode.crawlers.ratingandreviews.chile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 20/10/18
 * 
 * @author gabriel
 *
 */
public class ChileRipleyRatingReviewCrawler extends RatingReviewCrawler {

  public ChileRipleyRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject productJson = extractProductJson(doc);
      JSONArray products = productJson.has("SKUs") ? productJson.getJSONArray("SKUs") : new JSONArray();

      if (products.length() > 0) {

        JSONObject reviewStatistics = getReviewStatisticsJSON(productJson);
        Integer totalNumOfEvaluations = getTotalReviewCount(reviewStatistics);
        Double avgRating = getAverageOverallRating(reviewStatistics);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        for (int i = 0; i < products.length(); i++) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(crawlInternalId(products.getJSONObject(i)));
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("SKUUniqueID")) {
      internalId = skuJson.getString("SKUUniqueID");
    }

    return internalId;
  }

  private Integer getTotalReviewCount(JSONObject reviewStatistics) {
    Integer totalReviewCount = 0;
    if (reviewStatistics.has("fullReviews")) {
      String text = reviewStatistics.get("fullReviews").toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        totalReviewCount = Integer.parseInt(text);
      }
    }
    return totalReviewCount;
  }

  private Double getAverageOverallRating(JSONObject reviewStatistics) {
    Double avgOverallRating = 0d;
    if (reviewStatistics.has("averageOverallRating")) {
      avgOverallRating = CrawlerUtils.getDoubleValueFromJSON(reviewStatistics, "averageOverallRating");
    }
    return avgOverallRating;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private boolean isProductPage(Document doc) {
    return !doc.select(".product-item").isEmpty();
  }

  /**
   * 
   * @param doc
   * @return
   */
  private JSONObject extractProductJson(Document doc) {
    JSONObject productJson = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", ";", false, true);
    if (json.has("product")) {
      JSONObject state = json.getJSONObject("product");

      if (state.has("product")) {
        productJson = state.getJSONObject("product");
      }
    }

    return productJson;
  }

  private JSONObject getReviewStatisticsJSON(JSONObject productJson) {
    if (productJson.has("powerReview")) {
      return productJson.getJSONObject("powerReview");
    }

    return new JSONObject();
  }
}
