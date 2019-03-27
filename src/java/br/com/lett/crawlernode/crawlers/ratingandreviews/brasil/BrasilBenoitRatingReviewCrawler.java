package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
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
          Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(json, "RatingCount", 0);
          Double avgRating = CrawlerUtils.getDoubleValueFromJSON(model, "RatingAverage");

          ratingReviews.setInternalId(internalId);
          ratingReviews.setTotalRating(totalNumOfEvaluations);
          ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
          ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0d);
        }

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".x-product-top-main") != null;
  }

}
