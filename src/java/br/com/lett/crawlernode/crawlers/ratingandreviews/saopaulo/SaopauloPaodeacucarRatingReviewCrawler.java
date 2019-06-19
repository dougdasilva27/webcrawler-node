package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class SaopauloPaodeacucarRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloPaodeacucarRatingReviewCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.paodeacucar.com";

  @Override
  public void handleCookiesBeforeFetch() {

    // Criando cookie da loja 3 = São Paulo capital
    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", GPACrawler.SAO_PAULO_STORE_ID);
    cookie.setDomain(".paodeacucar.com");
    cookie.setPath("/");
    this.cookies.add(cookie);

  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject rating = crawlProductInformatioFromGPAApi(session.getOriginalURL());
      Integer totalNumOfEvaluations = getTotalNumOfRatings(rating);
      Integer totalReviews = getTotalNumOfReviews(rating);
      Double avgRating = getTotalAvgRating(rating);
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(rating);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));
      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }

    return ratingReviewsCollection;

  }


  private String crawlInternalId(String productUrl) {
    return CommonMethods.getLast(productUrl.replace(HOME_PAGE, "").split("produto/")).split("/")[0].trim();
  }

  /**
   * Average is in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(JSONObject rating) {
    Double avgRating = 0d;

    if (rating.has("average") && !rating.get("average").toString().equalsIgnoreCase("nan")) {
      avgRating = rating.getDouble("average");
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in key rating in json
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfReviews(JSONObject rating) {
    Integer totalReviews = 0;

    if (rating.has("rating")) {
      JSONObject ratingValues = rating.getJSONObject("rating");

      totalReviews = 0;

      for (int i = 1; i <= ratingValues.length(); i++) {
        if (ratingValues.has(Integer.toString(i))) {
          totalReviews += ratingValues.getInt(Integer.toString(i));
        }
      }
    }

    return totalReviews;
  }

  /**
   * Number of ratings appear in key rating in json
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(JSONObject rating) {
    Integer totalRating = null;

    if (rating.has("total")) {
      totalRating = rating.getInt("total");
    }

    return totalRating;
  }

  private boolean isProductPage(String url) {
    if (url.contains("paodeacucar.com/produto/")) {
      return true;
    }
    return false;
  }

  /**
   * Get the json of gpa api, this api has all info rating product
   * 
   * @return
   */
  private JSONObject crawlProductInformatioFromGPAApi(String productUrl) {
    JSONObject productsInfo = new JSONObject();

    String id;

    if (productUrl.contains("?")) {
      int x = productUrl.indexOf("produto/") + "produto/".length();
      int y = productUrl.indexOf("?", x);

      id = productUrl.substring(x, y);
    } else {
      int x = productUrl.indexOf("produto/") + "produto/".length();
      id = productUrl.substring(x);
    }

    if (id != null && id.contains("/")) {
      id = id.split("/")[0];
    }

    String url = "https://api.gpa.digital/pa/products/" + id + "/review";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject apiGPA = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (apiGPA.has("content")) {
      productsInfo = apiGPA.getJSONObject("content");
    }

    return productsInfo;
  }

  public static AdvancedRatingReview getTotalStarsFromEachValue(JSONObject rating) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    if (rating.has("rating")) {

      JSONObject histogram = rating.getJSONObject("rating");

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

}
