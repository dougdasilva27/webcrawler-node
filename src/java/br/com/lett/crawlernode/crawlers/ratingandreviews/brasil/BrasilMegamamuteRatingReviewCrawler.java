package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class BrasilMegamamuteRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilMegamamuteRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document, session.getOriginalURL())) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      JSONObject trustVoxResponse = requestTrustVoxEndpoint(crawlInternalPid(document));

      Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
      Double totalRating = getTotalRating(trustVoxResponse);

      Double avgRating = null;
      if (totalNumOfEvaluations > 0) {
        avgRating = MathUtils.normalizeTwoDecimalPlaces(totalRating / totalNumOfEvaluations);
      }

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      String[] internalIds = crawlInternalIds(document);

      for (String skuId : internalIds) {
        RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
        clonedRatingReviews.setInternalId(skuId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  /**
   * 
   * @return
   */
  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element elementPid = doc.select("#___rc-p-id").first();

    if (elementPid != null) {
      internalPid = elementPid.attr("value");
    }

    return internalPid;
  }

  /**
   * 
   * @param trustVoxResponse
   * @return the total of evaluations
   */
  private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
    if (trustVoxResponse.has("items")) {
      JSONArray ratings = trustVoxResponse.getJSONArray("items");
      return ratings.length();
    }
    return 0;
  }

  private Double getTotalRating(JSONObject trustVoxResponse) {
    Double totalRating = 0.0;
    if (trustVoxResponse.has("items")) {
      JSONArray ratings = trustVoxResponse.getJSONArray("items");

      for (int i = 0; i < ratings.length(); i++) {
        JSONObject rating = ratings.getJSONObject(i);

        if (rating.has("rate")) {
          totalRating += rating.getInt("rate");
        }
      }
    }
    return totalRating;
  }

  private JSONObject requestTrustVoxEndpoint(String id) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("http://trustvox.com.br/widget/opinions?code=");
    requestURL.append(id);

    requestURL.append("&");
    requestURL.append("store_id=1355");

    requestURL.append("&");
    requestURL.append(session.getOriginalURL());

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(DataFetcherNO.HTTP_HEADER_ACCEPT, "application/vnd.trustvox-v2+json");
    headerMap.put(DataFetcherNO.HTTP_HEADER_CONTENT_TYPE, "application/json; charset=utf-8");

    String response = GETFetcher.fetchPageGETWithHeaders(session, requestURL.toString(), null, headerMap, 1);

    JSONObject trustVoxResponse;
    try {
      trustVoxResponse = new JSONObject(response);
    } catch (JSONException e) {
      Logging.printLogWarn(logger, session, "Error creating JSONObject from trustvox response.");
      Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));

      trustVoxResponse = new JSONObject();
    }

    return trustVoxResponse;
  }

  private boolean isProductPage(Document document, String url) {
    return document.select("#___rc-p-sku-ids").first() != null && url.endsWith("/p");
  }

  private String[] crawlInternalIds(Document doc) {
    Element elementInternalId = doc.select("#___rc-p-sku-ids").first();
    String[] internalIds = null;

    if (elementInternalId != null) {
      internalIds = elementInternalId.attr("value").trim().split(",");
    }

    return internalIds;
  }

}
