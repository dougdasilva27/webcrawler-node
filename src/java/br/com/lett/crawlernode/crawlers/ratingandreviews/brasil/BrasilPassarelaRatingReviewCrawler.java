package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilPassarelaRatingReviewCrawler extends RatingReviewCrawler {

  private static final String HOME_PAGE = "https://www.passarela.com.br/";

  public BrasilPassarelaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected String handleURLBeforeFetch(String oldUrl) {
    String newUrl = oldUrl;
    String urlEnding = oldUrl.substring(oldUrl.indexOf(HOME_PAGE) + HOME_PAGE.length());

    newUrl = HOME_PAGE + "ccstoreui/v1/pages/" + urlEnding + "&dataOnly=false&cacheableDataOnly=true&productTypesRequired=true";
    return newUrl;
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject json = new JSONObject(doc.text());
    json = json.has("data") ? json.getJSONObject("data") : new JSONObject();
    json = json.has("page") ? json.getJSONObject("page") : new JSONObject();

    if (isProductPage(json)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      json = json.getJSONObject("product");
      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

      if (json.has("id")) {
        String internalPid = Integer.toString(json.getInt("id"));

        Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "da65c974-40a1-4fe6-9ef0-4110a813586d");
        Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        if (json.has("childSKUs")) {
          JSONArray variations = json.getJSONArray("childSKUs");

          for (Object o : variations) {
            JSONObject sku = (JSONObject) o;

            if (sku.has("repositoryId")) {
              String internalId = sku.getString("repositoryId");
              RatingsReviews clonedRatingReviews = ratingReviews.clone();
              clonedRatingReviews.setInternalId(internalId);
              ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
            }
          }
        }
      } else {
        System.err.println(json);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  protected boolean isProductPage(JSONObject json) {
    return json.has("product");
  }
}
