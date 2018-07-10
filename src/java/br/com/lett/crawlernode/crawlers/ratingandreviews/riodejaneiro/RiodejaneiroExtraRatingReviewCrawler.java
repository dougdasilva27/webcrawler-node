package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class RiodejaneiroExtraRatingReviewCrawler extends RatingReviewCrawler {

  public RiodejaneiroExtraRatingReviewCrawler(Session session) {
    super(session);
  }

  // Loja 42 rj
  private static final String STORE_ID = "42";

  private static final String HOME_PAGE = "https://www.deliveryextra.com.br";

  @Override
  public void handleCookiesBeforeFetch() {

    // Criando cookie da loja 3 = São Paulo capital
    BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", STORE_ID);
    cookie.setDomain(".deliveryextra.com");
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
      Double avgRating = getTotalAvgRating(rating);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(MathUtils.normalizeTwoDecimalPlaces(avgRating));
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
  private Integer getTotalNumOfRatings(JSONObject rating) {
    Integer totalRating = 0;

    if (rating.has("rating")) {
      JSONObject ratingValues = rating.getJSONObject("rating");

      totalRating = 0;

      for (int i = 1; i <= ratingValues.length(); i++) {
        if (ratingValues.has(Integer.toString(i))) {
          totalRating += ratingValues.getInt(Integer.toString(i));
        }
      }
    }

    return totalRating;
  }



  private boolean isProductPage(String url) {
    if (url.contains("deliveryextra.com.br/produto/")) {
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

    JSONObject apiGPA = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (apiGPA.has("content")) {
      productsInfo = apiGPA.getJSONObject("content");
    }

    return productsInfo;
  }

}
