package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;


public class TrustvoxRatingCrawler {

  private Session session;
  private String storeId;
  protected Logger logger;
  private String primaryImage;

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
  public RatingReviewsCollection extractRatingAndReviewsForVtex(Document document, DataFetcher dataFetcher) {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

    if (skuJson.has("productId")) {
      RatingsReviews ratingReviews = extractRatingAndReviews(skuJson.get("productId").toString(), document, dataFetcher);

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
  public RatingsReviews extractRatingAndReviews(String id, Document doc, DataFetcher dataFetcher) {
    RatingsReviews ratingReviews = new RatingsReviews();
    JSONObject trustVoxResponse = requestTrustVoxEndpoint(id, doc, dataFetcher);

    Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
    Double avgRating = getTotalRating(trustVoxResponse);
    AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(trustVoxResponse);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setDate(session.getDate());
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);

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

  public JSONObject requestTrustVoxEndpoint(String id, Document doc, DataFetcher dataFetcher) {
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
    } else {
      JSONObject normalStoreJson = selectJsonFromHtml(doc, "script", "_trustvox.push(['_productExtraAttributes',", "]);", true);
      if (normalStoreJson.has("group") && !normalStoreJson.isNull("group") && normalStoreJson.has("subgroup") && !normalStoreJson.isNull(
          "subgroup")) {
        requestURL.append("&product_extra_attributes%5Bgroup%5D=" + normalStoreJson.get("group") + "&product_extra_attributes%5Bsubgroup%5D="
            + normalStoreJson.get("subgroup"));
      }
    }

    if (this.primaryImage != null) {
      requestURL.append("&photos_urls%5B%5D=" + this.primaryImage);
    }

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(HttpHeaders.ACCEPT, "application/vnd.trustvox-v2+json");
    headerMap.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

    Request request = RequestBuilder.create().setUrl(requestURL.toString()).setHeaders(headerMap).build();
    String response = dataFetcher.get(session, request).getBody();

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
            Logging.printLogWarn(logger, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return object;
  }

  public static AdvancedRatingReview getTotalStarsFromEachValue(JSONObject trustVoxResponse) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    if (trustVoxResponse.has("histogram")) {
      JSONObject histogram = trustVoxResponse.getJSONObject("histogram");

      if (histogram.has("1") && histogram.get("1") instanceof Integer) {
        star1 = histogram.getInt("1");
      }

      if (histogram.has("2") && histogram.get("2") instanceof Integer) {
        star2 = histogram.getInt("2");
      }

      if (histogram.has("3") && histogram.get("3") instanceof Integer) {
        star3 = histogram.getInt("3");
      }

      if (histogram.has("4") && histogram.get("4") instanceof Integer) {
        star4 = histogram.getInt("4");
      }

      if (histogram.has("5") && histogram.get("5") instanceof Integer) {
        star5 = histogram.getInt("5");
      }
    }

    return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
  }

  public static AdvancedRatingReview getTotalStarsFromEachValueWithRate(JSONObject trustVoxResponse) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    if (trustVoxResponse.has("rate")) {
      JSONObject rate = trustVoxResponse.getJSONObject("rate");
      if (rate.has("histogram")) {

        JSONObject histogram = rate.getJSONObject("histogram");

        if (histogram.has("1") && histogram.get("1") instanceof Integer) {
          star1 = histogram.getInt("1");
        }

        if (histogram.has("2") && histogram.get("2") instanceof Integer) {
          star2 = histogram.getInt("2");
        }

        if (histogram.has("3") && histogram.get("3") instanceof Integer) {
          star3 = histogram.getInt("3");
        }

        if (histogram.has("4") && histogram.get("4") instanceof Integer) {
          star4 = histogram.getInt("4");
        }

        if (histogram.has("5") && histogram.get("5") instanceof Integer) {
          star5 = histogram.getInt("5");
        }
      }
    }

    return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
  }

  public String getPrimaryImage() {
    return primaryImage;
  }

  public void setPrimaryImage(String primaryImage) {
    this.primaryImage = primaryImage;
  }

}
