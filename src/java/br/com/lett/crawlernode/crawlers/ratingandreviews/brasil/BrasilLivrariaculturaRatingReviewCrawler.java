package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilLivrariaculturaRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilLivrariaculturaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(document);

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);

      String ratingApi = "https://comments.us1.gigya.com/comments.getComments?categoryID=ProductsRatingReview&streamID=" + internalId
          + "&includeStreamInfo=true&APIKey=3_3Mez5cLsMYm3EyiqY7w8i7fsPMonWe3pXEf29pFJTmxgG7pHbKZd0ytLh4KeenVe";

      JSONObject ratingJson = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, ratingApi, null, cookies);

      ratingReviews.setAverageOverallRating(crawlRatingAvg(ratingJson));

      Integer countRating = crawlRatingCount(ratingJson);
      ratingReviews.setTotalRating(countRating);
      ratingReviews.setTotalWrittenReviews(countRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.select("#product-highlights").first() != null;
  }


  private String crawlInternalId(Document document) {
    String internalId = null;

    Elements scripts = document.select("script");

    for (Element e : scripts) {
      String script = e.html().replace(" ", "").toLowerCase();

      if (script.startsWith("varproductid=")) {
        internalId = script.split("=")[1].replace("'", "").replace(";", "").trim();
        break;
      }
    }

    return internalId;
  }

  private Integer crawlRatingCount(JSONObject ratingJson) {
    Integer countRating = 0;

    if (ratingJson.has("streamInfo")) {
      JSONObject streamInfo = ratingJson.getJSONObject("streamInfo");

      if (streamInfo.has("ratingCount") && streamInfo.get("ratingCount") instanceof Integer) {
        countRating = streamInfo.getInt("ratingCount");
      }
    }

    return countRating;
  }

  private Double crawlRatingAvg(JSONObject ratingJson) {
    Double avgRating = 0d;

    if (ratingJson.has("streamInfo")) {
      JSONObject streamInfo = ratingJson.getJSONObject("streamInfo");

      if (streamInfo.has("avgRatings")) {
        JSONObject avgRatings = streamInfo.getJSONObject("avgRatings");

        if (avgRatings.has("_overall") && avgRatings.get("_overall") instanceof Double) {
          avgRating = avgRatings.getDouble("_overall");
        }
      }
    }

    return avgRating;
  }

}
