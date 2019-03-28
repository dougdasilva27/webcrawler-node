package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class SaopauloDrogaraiaRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloDrogaraiaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      String internalId = crawlInternalId(document);

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);

      JSONObject trustVoxResponse = requestTrustVoxEndpoint(internalId);
      Integer total = getTotalNumOfRatings(trustVoxResponse);

      ratingReviews.setTotalRating(total);
      ratingReviews.setTotalWrittenReviews(total);
      ratingReviews.setAverageOverallRating(getTotalRating(trustVoxResponse));

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  /**
   * 
   * @param trustVoxResponse
   * @return the total of evaluations
   */
  private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
    if (trustVoxResponse.has("rate")) {
      JSONObject rate = trustVoxResponse.getJSONObject("rate");

      if (rate.has("count")) {
        return rate.getInt("count");
      }
    }
    return 0;
  }

  private Double getTotalRating(JSONObject trustVoxResponse) {
    Double totalRating = 0.0;
    if (trustVoxResponse.has("rate")) {
      JSONObject rate = trustVoxResponse.getJSONObject("rate");

      if (rate.has("average")) {
        totalRating = rate.getDouble("average");
      }
    }

    return totalRating;
  }

  private JSONObject requestTrustVoxEndpoint(String id) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("http://trustvox.com.br/widget/root?code=");
    requestURL.append(id);

    requestURL.append("&");
    requestURL.append("store_id=71450");

    requestURL.append("&");
    try {
      requestURL.append(URLEncoder.encode(session.getOriginalURL(), "UTF-8"));
    } catch (UnsupportedEncodingException e1) {
      Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
    }

    requestURL.append("&product_extra_attributes%5Bsubgroup%5D");

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(HttpHeaders.ACCEPT, "application/vnd.trustvox-v2+json");
    headerMap.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

    Request request = RequestBuilder.create().setUrl(requestURL.toString()).setCookies(cookies).setHeaders(headerMap).build();
    String response = this.dataFetcher.get(session, request).getBody();

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

  private boolean isProductPage(Document document) {
    return !document.select(".product-name h1").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", true, false);

    if (json.has("ecommerce")) {
      JSONObject ecommerce = json.getJSONObject("ecommerce");

      if (ecommerce.has("detail")) {
        JSONObject detail = ecommerce.getJSONObject("detail");

        if (detail.has("products")) {
          JSONArray products = detail.getJSONArray("products");

          if (products.length() > 0) {
            JSONObject product = products.getJSONObject(0);

            if (product.has("id")) {
              internalId = product.getString("id");
            }
          }
        }
      }
    }

    return internalId;
  }

}
