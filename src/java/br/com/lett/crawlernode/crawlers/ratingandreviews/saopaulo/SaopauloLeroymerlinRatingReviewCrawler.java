package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloLeroymerlinRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloLeroymerlinRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      String internalId = crawlInternalId(document);

      JSONObject trustVoxResponse = requestTrustVoxEndpoint(internalId, document);
      Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
      Double avgRating = getTotalRating(trustVoxResponse);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-code").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.selectFirst(".product-code");
    if (internalIdElement != null) {
      String text = internalIdElement.text();

      if (text.contains(".")) {
        internalId = CommonMethods.getLast(text.split("\\.")).trim();
      } else if (text.contains("digo")) {
        internalId = CommonMethods.getLast(text.split("digo")).trim();
      } else {
        internalId = text.trim();
      }
    }

    return internalId;
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

  private JSONObject requestTrustVoxEndpoint(String id, Document doc) {
    StringBuilder requestURL = new StringBuilder();

    requestURL.append("https://trustvox.com.br/widget/root?code=");
    requestURL.append(id);

    requestURL.append("&");
    requestURL.append("store_id=73447");

    requestURL.append("&url=");
    requestURL.append(session.getOriginalURL());

    JSONObject vtxctx = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtxctx=", ";", true, false);

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
}
