package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

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
import models.RatingsReviews;

public class BrasilEpocacosmeticosRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilEpocacosmeticosRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

      if (skuJson.has("productId")) {
        JSONObject trustVoxResponse = requestTrustVoxEndpoint(skuJson.getInt("productId"), document);

        Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
        Double avgRating = getTotalRating(trustVoxResponse);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
        ratingReviews.setDate(session.getDate());

        List<String> ids = crawlIdList(skuJson);

        for (String internalId : ids) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }

      }

    }

    return ratingReviewsCollection;

  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private JSONObject requestTrustVoxEndpoint(int id, Document doc) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("http://trustvox.com.br/widget/root?code=");
    requestURL.append(id);

    requestURL.append("&");
    requestURL.append("store_id=393");

    requestURL.append("&url=");
    requestURL.append(session.getOriginalURL());

    JSONObject vtxctx = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtxctx=", ";", true, true);

    if (vtxctx.has("departmentyId") && vtxctx.has("categoryId")) {
      requestURL.append("&product_extra_attributes%5Bdepartment_id%5D=" + vtxctx.get("departmentyId") + "&product_extra_attributes%5Bcategory_id%5D="
          + vtxctx.get("categoryId"));
    }

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(DataFetcher.HTTP_HEADER_ACCEPT, "application/vnd.trustvox-v2+json");
    headerMap.put(DataFetcher.HTTP_HEADER_CONTENT_TYPE, "application/json; charset=utf-8");

    String response = GETFetcher.fetchPageGETWithHeaders(session, requestURL.toString(), null, headerMap, 1);

    JSONObject trustVoxResponse;
    try {
      trustVoxResponse = new JSONObject(response);

      if (trustVoxResponse.has("rate")) {
        return trustVoxResponse.getJSONObject("rate");
      }

    } catch (JSONException e) {
      Logging.printLogWarn(logger, session, "Error creating JSONObject from trustvox response.");
      Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));

      trustVoxResponse = new JSONObject();
    }

    return trustVoxResponse;
  }

  private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
    if (trustVoxResponse.has("count") && trustVoxResponse.get("count") instanceof Integer) {
      return trustVoxResponse.getInt("count");
    }
    return 0;
  }

  private Double getTotalRating(JSONObject trustVoxResponse) {
    if (trustVoxResponse.has("average") && trustVoxResponse.get("average") instanceof Double) {
      return trustVoxResponse.getDouble("average");
    }
    return 0d;
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

}
