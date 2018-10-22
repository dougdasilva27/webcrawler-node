package br.com.lett.crawlernode.crawlers.ratingandreviews.chile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

/**
 * Date: 20/10/18
 * 
 * @author gabriel
 *
 */
public class ChileFalabellaRatingReviewCrawler extends RatingReviewCrawler {

  public ChileFalabellaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject productJson = extractProductJson(doc);
      JSONArray products = productJson.has("skus") ? productJson.getJSONArray("skus") : new JSONArray();

      if (products.length() > 0) {

        String internalPid = crawlInternalPid(productJson);
        final String bazaarVoicePassKey = "mk9fosfh4vxv20y8u5pcbwipl";
        String endpointRequest = assembleBazaarVoiceEndpointRequest(internalPid, bazaarVoicePassKey, 0, 50);

        JSONObject ratingReviewsEndpointResponse = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, endpointRequest, null, null);
        JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, internalPid);

        Integer totalNumOfEvaluations = getTotalReviewCount(reviewStatistics);
        Double avgRating = getAverageOverallRating(reviewStatistics);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        for (int i = 0; i < products.length(); i++) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(crawlInternalId(products.getJSONObject(i)));
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }

    }

    return ratingReviewsCollection;

  }

  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("id")) {
      internalPid = productJson.get("id").toString();
    }

    return internalPid;
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("skuId")) {
      internalId = skuJson.getString("skuId");
    }

    return internalId;
  }

  private Integer getTotalReviewCount(JSONObject reviewStatistics) {
    Integer totalReviewCount = 0;
    if (reviewStatistics.has("TotalReviewCount")) {
      totalReviewCount = reviewStatistics.getInt("TotalReviewCount");
    }
    return totalReviewCount;
  }

  private Double getAverageOverallRating(JSONObject reviewStatistics) {
    Double avgOverallRating = 0d;
    if (reviewStatistics.has("AverageOverallRating")) {
      avgOverallRating = reviewStatistics.getDouble("AverageOverallRating");
    }
    return avgOverallRating;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private boolean isProductPage(Document doc) {
    return !doc.select(".fb-product__form").isEmpty();
  }

  /**
   * 
   * @param doc
   * @return
   */
  private JSONObject extractProductJson(Document doc) {
    JSONObject productJson = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var fbra_browseMainProductConfig =", ";", false, false);
    if (json.has("state")) {
      JSONObject state = json.getJSONObject("state");

      if (state.has("product")) {
        productJson = state.getJSONObject("product");
      }
    }

    return productJson;
  }

  /**
   * e.g: http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4
   * &passkey=oqu6lchjs2mb5jp55bl55ov0d &Offset=0 &Limit=5 &Sort=SubmissionTime:desc
   * &Filter=ProductId:113048617 &Include=Products &Stats=Reviews
   * 
   * Endpoint request parameters:
   * <p>
   * &passKey: the password used to request the bazaar voice endpoint. This pass key e crawled inside
   * the html of the sku page, inside a script tag. More details on how to crawl this passKey
   * </p>
   * <p>
   * &Offset: the number of the chunk of data retrieved by the endpoint. If we want the second chunk,
   * we must add this value by the &Limit parameter.
   * </p>
   * <p>
   * &Limit: the number of reviews that a request will return, at maximum.
   * </p>
   * 
   * The others parameters we left as default.
   * 
   * Request Method: GET
   */
  private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

    StringBuilder request = new StringBuilder();

    request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.4");
    request.append("&passkey=" + bazaarVoiceEnpointPassKey);
    request.append("&Offset=" + offset);
    request.append("&Limit=" + limit);
    request.append("&Sort=SubmissionTime:desc");
    request.append("&Filter=ProductId:" + skuInternalPid);
    request.append("&Include=Products");
    request.append("&Stats=Reviews");

    return request.toString();
  }


  private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
    if (ratingReviewsEndpointResponse.has("Includes")) {
      JSONObject includes = ratingReviewsEndpointResponse.getJSONObject("Includes");

      if (includes.has("Products")) {
        JSONObject products = includes.getJSONObject("Products");

        if (products.has(skuInternalPid)) {
          JSONObject product = products.getJSONObject(skuInternalPid);

          if (product.has("ReviewStatistics")) {
            return product.getJSONObject("ReviewStatistics");
          }
        }
      }
    }

    return new JSONObject();
  }
}
