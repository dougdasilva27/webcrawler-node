package br.com.lett.crawlernode.crawlers.ratingandreviews.curitiba;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class CuritibaMuffatoRatingReviewCrawler extends RatingReviewCrawler {

  public CuritibaMuffatoRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(doc);
      JSONObject rating = crawlProductInformatioApi(internalId);

      Integer totalReviews = getTotalNumOfReviews(rating, internalId);
      Double avgRating = getTotalAvgRating(rating, internalId);

      ratingReviews.setTotalRating(totalReviews);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setInternalId(internalId);
      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
    if (elementInternalID != null) {
      internalId = elementInternalID.text();
    }

    return internalId;
  }

  private Double getTotalAvgRating(JSONObject rating, String internalId) {
    JSONArray ratingArray = rating.has(internalId) ? rating.getJSONArray(internalId) : new JSONArray();
    Double avg = null;
    for (Object object : ratingArray) {
      JSONObject jsonRating = (JSONObject) object;
      if (jsonRating.has("rate")) {
        avg = MathUtils.parseDoubleWithDot(jsonRating.get("rate").toString());
      }
    }
    return avg;
  }

  private Integer getTotalNumOfReviews(JSONObject rating, String internalId) {
    JSONArray ratingArray = rating.has(internalId) ? rating.getJSONArray(internalId) : new JSONArray();
    Integer total = null;
    for (Object object : ratingArray) {
      JSONObject jsonRating = (JSONObject) object;
      if (jsonRating.has("count")) {
        total = jsonRating.getInt("count");
      }
    }
    return total;
  }

  private JSONObject crawlProductInformatioApi(String internalId) {
    String apiUrl = "https://awsapis3.netreviews.eu/product";
    String payload =
        "{\"query\":\"average\",\"products\":[\"" + internalId + "\"],\"idWebsite\":\"4f870cb3-d6ef-5664-2950-de136d5b471e\",\"plateforme\":\"br\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=UTF-8");
    headers.put("Content-Encoding", "");

    return CrawlerUtils.stringToJson(POSTFetcher.requestStringUsingFetcher(apiUrl, cookies, headers, payload, "POST", session, false));

  }

  private boolean isProductPage(Document document) {
    return document.select(".container.prd-info-container").first() != null;
  }
}
