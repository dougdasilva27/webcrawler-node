package br.com.lett.crawlernode.crawlers.ratingandreviews.models;

import java.util.Optional;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class GPARatingCrawler extends RatingReviewCrawler {

  protected String storeId;
  protected String store;
  protected String cep;
  protected String homePageHttps;

  private static final String END_POINT_REQUEST = "https://api.gpa.digital/";

  public GPARatingCrawler(Session session) {
    super(session);
    inferFields();
  }

  @Override
  public void handleCookiesBeforeFetch() {
    fetchStoreId();
    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", this.storeId);
    cookie.setDomain(homePageHttps.substring(homePageHttps.indexOf("www"), homePageHttps.length() - 1));
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  private void inferFields() {
    String className = this.getClass().getSimpleName().toLowerCase();
    if (className.contains("paodeacucar")) {
      this.store = "pa";
      this.homePageHttps = "https://www.paodeacucar.com/";
    } else if (className.contains("extra")) {
      this.store = "ex";
      this.homePageHttps = "https://www.clubeextra.com.br/";
    }
  }

  private void fetchStoreId() {
    Request request = RequestBuilder.create()
        .setUrl(END_POINT_REQUEST + this.store + "/delivery/options?zipCode=" + this.cep.replace("-", ""))
        .setCookies(cookies)
        .build();

    Response response = this.dataFetcher.get(session, request);

    JSONObject jsonObjectGPA = JSONUtils.stringToJson(response.getBody());

    Optional<JSONObject> optionalJson = Optional.of(jsonObjectGPA);
    if (optionalJson.isPresent()) {
      JSONArray jsonDeliveryTypes = optionalJson
          .map(x -> x.optJSONObject("content"))
          .map(x -> x.optJSONArray("deliveryTypes"))
          .get();
      if (jsonDeliveryTypes.optJSONObject(0) != null) {
        this.storeId = jsonDeliveryTypes.optJSONObject(0).optString("storeid");
      }
      for (Object object : jsonDeliveryTypes) {
        JSONObject deliveryType = (JSONObject) object;
        if (deliveryType.optString("name") != null && deliveryType.optString("name").contains("TRADICIONAL")) {
          this.storeId = deliveryType.optString("storeid");
          break;
        }
      }
    }
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

    String url = END_POINT_REQUEST + this.store + "/products/" + id + "/review";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject apiGPA = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (apiGPA.has("content")) {
      productsInfo = apiGPA.getJSONObject("content");
    }

    return productsInfo;
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

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(rating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }
    return ratingReviewsCollection;
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


  private String crawlInternalId(String productUrl) {
    return CommonMethods.getLast(productUrl.replace(this.homePageHttps, "").split("produto/")).split("/")[0];
  }

  /**
   * Average is in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(JSONObject rating) {
    Double avgRating = 0D;

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
    return rating.getInt("total");
  }

  private boolean isProductPage(String url) {
    return url.contains(this.homePageHttps + "produto/");
  }
}
