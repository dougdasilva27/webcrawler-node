package br.com.lett.crawlernode.crawlers.ratingandreviews.unitedstates;

import java.util.HashMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 22/02/19
 * 
 * @author gabriel
 *
 */
public class UnitedstatesWalgreensRatingReviewCrawler extends RatingReviewCrawler {

  private static final String HOME_PAGE = "https://www.walgreens.com/";

  public UnitedstatesWalgreensRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null, ".walgreens.com", "/", cookies, session, new HashMap<>());
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject results = scrapProductInfo(doc);

    if (results.has("productInfo")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productInfo = results.getJSONObject("productInfo");
      String internalId = productInfo.has("skuId") ? productInfo.get("skuId").toString() : null;

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private JSONObject scrapProductInfo(Document doc) {
    JSONObject productInfo = new JSONObject();

    JSONObject initialState = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APP_INITIAL_STATE__=", "};", true, false);
    if (initialState.has("product")) {
      JSONObject product = initialState.getJSONObject("product");

      if (product.has("results")) {
        productInfo = product.getJSONObject("results");
      }
    }

    return productInfo;
  }

  /**
   * Avg appear in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;
    Element reviews = doc.selectFirst("#reviewsData .pr10");

    if (reviews != null) {
      String text = reviews.ownText().replaceAll("[^0-9.]", "").trim();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in html element
   * 
   * @param doc
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    Integer ratingNumber = 0;
    Element reviews = doc.selectFirst("#reviewsData .ml10");

    if (reviews != null) {
      String text = reviews.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        ratingNumber = Integer.parseInt(text);
      }
    }

    return ratingNumber;
  }
}
