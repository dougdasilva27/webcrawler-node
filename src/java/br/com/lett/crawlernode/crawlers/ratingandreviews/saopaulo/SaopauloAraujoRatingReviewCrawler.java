package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloAraujoRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloAraujoRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

      if (skuJson.has("productId")) {
        JSONObject trustVoxResponse = requestTrustVoxEndpoint(skuJson.get("productId").toString(), skuJson, document);

        Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
        Double avgRating = getTotalRating(trustVoxResponse);
        AdvancedRatingReview advancedRatingReview = TrustvoxRatingCrawler.getTotalStarsFromEachValue(trustVoxResponse);

        ratingReviews.setAdvancedRatingReview(advancedRatingReview);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setDate(session.getDate());

        List<String> idList = crawlIdList(skuJson);
        for (String internalId : idList) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }

    }

    return ratingReviewsCollection;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".skuList").isEmpty();
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

  private JSONObject requestTrustVoxEndpoint(String id, JSONObject skuJson, Document doc) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("https://trustvox.com.br/widget/root?");

    String name = scrapName(skuJson, "name");
    String photos = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image a[href]", Arrays.asList("href"), "https", "www.araujo.com.br");

    requestURL.append("code=" + id);
    requestURL.append("&store_id=78444");
    requestURL.append("&url=" + CommonMethods.encondeStringURLToISO8859(session.getOriginalURL(), logger, session));

    if (name != null) {
      requestURL.append("&name=" + CommonMethods.encondeStringURLToISO8859(name.replace("+", ""), logger, session));
    }

    if (photos != null) {
      requestURL.append("&photos_urls%5B%5D==" + CommonMethods.encondeStringURLToISO8859(photos.split("\\?")[0], logger, session));
    }

    JSONObject vtxctx = selectJsonFromHtml(doc, "script", "vtxctx=", ";", true);

    if (vtxctx.has("departmentyId") && vtxctx.has("categoryId")) {
      requestURL.append("&product_extra_attributes%5Bdepartment_id%5D=" + vtxctx.get("departmentyId") + "&product_extra_attributes%5Bcategory_id%5D="
          + vtxctx.get("categoryId"));
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

  private String scrapName(JSONObject productJson, String key) {
    String field = null;

    if (productJson.has(key) && !productJson.isNull(key)) {
      field = productJson.get(key).toString();
    }

    return field;
  }

  public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces)
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
}
