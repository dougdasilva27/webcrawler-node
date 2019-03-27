package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilBuscapeRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilBuscapeRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject pageInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "window.__INITIAL_STATE__ =", ";", false, true);
      JSONObject ratingJson =
          pageInfo.has("aggregateRating") && !pageInfo.isNull("aggregateRating") ? pageInfo.getJSONObject("aggregateRating") : new JSONObject();

      Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(ratingJson, "reviewCount", 0);
      Double avgRating = CrawlerUtils.getDoubleValueFromJSON(ratingJson, "ratingValue", true, false);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0d);
      ratingReviews.setInternalId(scrapInternalId(pageInfo));
      ratingReviewsCollection.addRatingReviews(ratingReviews);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product").isEmpty();
  }

  private String scrapInternalId(JSONObject pageInfo) {
    String internalId = null;

    if (pageInfo.has("product")) {
      JSONObject productInfo = pageInfo.getJSONObject("product");
      internalId = productInfo.has("id") ? productInfo.get("id").toString() : null;
    }

    return internalId;
  }
}
