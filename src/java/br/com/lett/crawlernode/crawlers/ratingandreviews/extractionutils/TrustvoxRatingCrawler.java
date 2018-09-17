package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;


public class TrustvoxRatingCrawler {

  private Session session;
  private String storeId;
  protected Logger logger;

  public TrustvoxRatingCrawler(Session session, String storeId, Logger logger) {
    this.session = session;
    this.storeId = storeId;
    this.logger = logger;
  }

  /**
   * Extract rating info from trustVox API for vtex sites
   * 
   * @param document - html
   * @return
   */
  public RatingReviewsCollection extractRatingAndReviewsForVtex(Document document) {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

    if (skuJson.has("productId")) {
      RatingsReviews ratingReviews = extractRatingAndReviews(skuJson.get("productId").toString(), document);

      List<String> idList = VTEXCrawlersUtils.crawlIdList(skuJson);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }
    }

    return ratingReviewsCollection;
  }

  /**
   * Extract rating info from trustVox API
   * 
   * @param id - product Id
   * @param doc - html
   * @return
   */
  public RatingsReviews extractRatingAndReviews(String id, Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    JSONObject trustVoxResponse = requestTrustVoxEndpoint(id, doc);

    Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
    Double avgRating = getTotalRating(trustVoxResponse);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setDate(session.getDate());

    return ratingReviews;
  }

  public Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
    String countKey = "count";

    if (trustVoxResponse.has(countKey) && trustVoxResponse.get(countKey) instanceof Integer) {
      return trustVoxResponse.getInt(countKey);
    }
    return 0;
  }

  public Double getTotalRating(JSONObject trustVoxResponse) {
    String averageKey = "average";

    if (trustVoxResponse.has(averageKey) && trustVoxResponse.get(averageKey) instanceof Double) {
      return trustVoxResponse.getDouble(averageKey);
    }
    return 0d;
  }

  public JSONObject requestTrustVoxEndpoint(String id, Document doc) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("https://trustvox.com.br/widget/root?code=");
    requestURL.append(id);

    requestURL.append("&");
    requestURL.append("store_id=" + this.storeId);

    requestURL.append("&url=");
    requestURL.append(session.getOriginalURL());

    JSONObject vtxctx = selectJsonFromHtml(doc, "script", "vtxctx=", ";", true);

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
      Logging.printLogError(logger, session, "Error creating JSONObject from trustvox response.");
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

      trustVoxResponse = new JSONObject();
    }

    return trustVoxResponse;
  }

  private JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces)
      throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {

    if (doc == null)
      throw new IllegalArgumentException("Argument doc cannot be null");

    JSONObject object = new JSONObject();

    Elements scripts = doc.select(cssElement);

    for (Element e : scripts) {
      String script = e.html();

      script = withoutSpaces ? script.replace(" ", "") : script;

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();

        String json;

        if (script.contains(finalIndex)) {
          int y = script.lastIndexOf(finalIndex);
          json = script.substring(x, y).trim();
        } else {
          json = script.substring(x).trim();
        }

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            object = new JSONObject(json);
          } catch (Exception e1) {
            Logging.printLogError(logger, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return object;
  }
}
