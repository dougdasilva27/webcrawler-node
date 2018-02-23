package br.com.lett.crawlernode.crawlers.ratingandreviews.belohorizonte;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BelohorizonteSupernossoRatingReviewCrawler extends RatingReviewCrawler {

  public BelohorizonteSupernossoRatingReviewCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.supernossoemcasa.com.br/e-commerce/";

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    // performing request to get cookie
    String cookieValue = DataFetcher.fetchCookie(session, HOME_PAGE, "JSESSIONID", null, 1);

    BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", cookieValue);
    cookie.setDomain("www.supernossoemcasa.com.br");
    cookie.setPath("/e-commerce/");
    this.cookies.add(cookie);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject api = crawlProductInformatioFromApi(session.getOriginalURL());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(api);
      Double avgRating = getTotalAvgRating(api);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));
      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }

    return ratingReviewsCollection;
  }

  private String crawlInternalId(String productUrl) {
    return productUrl.replace(HOME_PAGE, "").split("/")[2];
  }

  /**
   * Average is in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(JSONObject rating) {
    Double avgRating = null;

    if (rating.has("ratingAverage")) {
      Object avg = rating.get("ratingAverage");

      if (avg instanceof Double) {
        avgRating = (Double) avg;
      } else if (avg instanceof Integer) {
        avgRating = ((Integer) avg).doubleValue();
      }
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in key rating in json
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(JSONObject api) {
    Integer totalRating = null;

    if (api.has("ratingCount") && api.get("ratingCount") instanceof Integer) {
      totalRating = api.getInt("ratingCount");
    }

    return totalRating;
  }



  private boolean isProductPage(String url) {
    return url.startsWith(HOME_PAGE + "p/");
  }

  private JSONObject crawlProductInformatioFromApi(String productUrl) {
    JSONObject api = new JSONObject();

    if (productUrl.contains("/p/")) {
      String id = productUrl.split("/p/")[1].split("/")[0];
      String apiUrl = "https://www.supernossoemcasa.com.br/e-commerce/api/products/" + id;

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

      // request with fetcher
      JSONObject fetcherResponse = POSTFetcher.fetcherRequest(apiUrl, cookies, headers, null, DataFetcher.GET_REQUEST, session, false);
      String page = null;

      if (fetcherResponse.has("response") && fetcherResponse.has("request_status_code") && fetcherResponse.getInt("request_status_code") >= 200
          && fetcherResponse.getInt("request_status_code") < 400) {
        JSONObject response = fetcherResponse.getJSONObject("response");

        if (response.has("body")) {
          page = response.getString("body");
        }
      } else {
        // normal request
        page = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, apiUrl, null, cookies);
      }

      if (page != null && page.startsWith("{") && page.endsWith("}")) {
        try {
          api = new JSONObject(page);
        } catch (Exception e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    return api;
  }

}
