package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class SaopauloMamboRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloMamboRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

      if (skuJson.has("productId")) {
        JSONObject trustVoxResponse = requestTrustVoxEndpoint(skuJson.getInt("productId"));

        Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
        Double totalRating = getTotalRating(trustVoxResponse);

        Double avgRating = null;
        if (totalNumOfEvaluations > 0) {
          avgRating = MathUtils.normalizeTwoDecimalPlaces(totalRating / totalNumOfEvaluations);
        }

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setDate(session.getDate());

        List<String> idList = crawlIdList(skuJson);
        for (String internalId : idList) {
          RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }

    }

    return ratingReviewsCollection;

  }

  private List<String> crawlIdList(JSONObject skuJson) {
    List<String> idList = new ArrayList<>();

    if (skuJson.has("skus")) {
      JSONArray skus = skuJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("sku")) {
          idList.add(Integer.toString(sku.getInt("sku")));
        }
      }
    }

    return idList;
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

  private JSONObject requestTrustVoxEndpoint(int id) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("http://trustvox.com.br/widget/opinions?code=");
    requestURL.append(id);

    requestURL.append("&");
    requestURL.append("store_id=944");

    requestURL.append("&");
    requestURL.append(session.getOriginalURL());

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(DataFetcher.HTTP_HEADER_ACCEPT, "application/vnd.trustvox-v2+json");
    headerMap.put(DataFetcher.HTTP_HEADER_CONTENT_TYPE, "application/json; charset=utf-8");

    String response = GETFetcher.fetchPageGETWithHeaders(session, requestURL.toString(), null, headerMap, 1);

    JSONObject trustVoxResponse;
    try {
      trustVoxResponse = new JSONObject(response);
    } catch (JSONException e) {
      Logging.printLogError(logger, session, "Error creating JSONObject from trustvox response.");
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

      trustVoxResponse = new JSONObject();
    }

    return trustVoxResponse;
  }

  private boolean isProductPage(Document document) {
    return document.select(".produto").first() != null;
  }
}
