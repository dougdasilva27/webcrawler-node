package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilBenoitRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilBenoitRatingReviewCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String url = session.getOriginalURL().concat(".json");
    JSONObject json = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      if (json.has("Model") && !json.isNull("Model")) {
        JSONObject model = json.getJSONObject("Model");
        String internalId = model.has("ProductID") ? model.get("ProductID").toString() : null;
        if (internalId != null) {



          Integer totalNumOfEvaluations = getTotalNumOfRatings(model);
          Double avgRating = getTotalAvgRating(model);

          ratingReviews.setInternalId(internalId);
          ratingReviews.setTotalRating(totalNumOfEvaluations);
          ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
          ratingReviews.setAverageOverallRating(avgRating);
        }

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }



  private Double getTotalAvgRating(JSONObject model) {
    Double totalAvg = 0d;

    if (model.has("RatingAverage") && !model.isNull("RatingAverage")) {
      totalAvg = model.getDouble("RatingAverage");
    }

    return totalAvg;
  }

  private Integer getTotalNumOfRatings(JSONObject model) {
    Integer count = 0;

    if (model.has("RatingCount") && !model.isNull("RatingCount")) {
      count = model.getInt("RatingCount");
    }

    return count;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".x-product-top-main") != null;
  }

}
