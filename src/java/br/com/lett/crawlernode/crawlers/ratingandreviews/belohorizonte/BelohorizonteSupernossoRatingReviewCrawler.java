package br.com.lett.crawlernode.crawlers.ratingandreviews.belohorizonte;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
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

    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, Arrays.asList("JSESSIONID"), "www.supernossoemcasa.com.br", "/e-commerce/", cookies,
        session, new HashMap<>(), dataFetcher);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject api = crawlProductInformatioFromApi();

      Integer totalNumOfEvaluations = getTotalNumOfRatings(api);
      Double avgRating = getTotalAvgRating(api);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setInternalId(crawlInternalId(api));
      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }

    return ratingReviewsCollection;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku");
    }

    return internalId;
  }

  /**
   * Average is in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(JSONObject rating) {
    Double avgRating = 0d;

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
    Integer totalRating = 0;

    if (api.has("ratingCount") && api.get("ratingCount") instanceof Integer) {
      totalRating = api.getInt("ratingCount");
    }

    return totalRating;
  }



  private boolean isProductPage(String url) {
    return url.startsWith(HOME_PAGE + "p/");
  }

  private JSONObject crawlProductInformatioFromApi() {
    JSONObject api = new JSONObject();

    String url = session.getOriginalURL();

    if (url.contains("/p/")) {
      String id = url.split("/p/")[1].split("/")[0];
      String apiUrl = "https://www.supernossoemcasa.com.br/e-commerce/api/products/" + id;

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "application/json, text/javascript, */*; q=0.01");

      // request with fetcher
      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).build();
      String page = this.dataFetcher.get(session, request).getBody();

      if (page == null || page.isEmpty()) {
        page = new ApacheDataFetcher().get(session, request).getBody();
      }

      api = CrawlerUtils.stringToJson(page);
    }

    return api;
  }

}
