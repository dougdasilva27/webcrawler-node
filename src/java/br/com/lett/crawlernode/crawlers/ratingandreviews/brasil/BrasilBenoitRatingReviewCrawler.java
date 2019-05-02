package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      if (json.has("Model") && !json.isNull("Model")) {
        JSONObject model = json.getJSONObject("Model");

        Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(json, "RatingCount", 0);
        Double avgRating = CrawlerUtils.getDoubleValueFromJSON(model, "RatingAverage");

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0d);

        JSONArray items = model.has("Items") ? model.getJSONArray("Items") : new JSONArray();

        for (Object obj : items) {
          JSONObject sku = (JSONObject) obj;

          // This verification exists to the json don't return the empty object.
          if (sku.has("Items") && sku.getJSONArray("Items").length() < 1) {
            String internalId = sku.has("ProductID") ? sku.get("ProductID").toString() : null;
            ratingReviews.setInternalId(internalId);
            RatingsReviews clonedRatingReviews = ratingReviews.clone();
            clonedRatingReviews.setInternalId(internalId);
            ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
          }
        }
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
