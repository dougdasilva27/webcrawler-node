package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.HashMap;
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

public class SaopauloDrogasilRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloDrogasilRatingReviewCrawler(Session session) {
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

      ratingReviews.setTotalRating(getTotalNumOfRatings(trustVoxResponse));
      ratingReviews.setAverageOverallRating(getTotalRating(trustVoxResponse));

      ratingReviewsCollection.addRatingReviews(ratingReviews);


    }

    return ratingReviewsCollection;

  }

  /**
   * 
   * @param trustVoxResponse
   * @return the total of evaluations
   */
  private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
    Integer total = 0;

    if (trustVoxResponse.has("rate")) {
      JSONObject rate = trustVoxResponse.getJSONObject("rate");

      if (rate.has("count")) {
        total = rate.getInt("count");
      }
    }

    return total;
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
    requestURL.append("store_id=71447");

    requestURL.append("&url=");
    requestURL.append(session.getOriginalURL());

    requestURL.append("&product_extra_attributes%5Bgroup%5D=PERFUMARIA");

    Map<String, String> headerMap = new HashMap<>();
    headerMap.put(DataFetcher.HTTP_HEADER_ACCEPT, "application/vnd.trustvox-v2+json");

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
    return document.select("#details .col-2 .data-table tr .data").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");");

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
