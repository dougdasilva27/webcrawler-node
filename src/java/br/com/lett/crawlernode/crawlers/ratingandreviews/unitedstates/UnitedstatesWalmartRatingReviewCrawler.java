package br.com.lett.crawlernode.crawlers.ratingandreviews.unitedstates;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.UnitedstatesWalmartCrawlerUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class UnitedstatesWalmartRatingReviewCrawler extends RatingReviewCrawler {

  public UnitedstatesWalmartRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("akgeo", "US");
    cookie.setDomain("www.walmart.com");
    cookie.setPath("/");
    this.cookies.add(cookie);

    BasicClientCookie cookie2 = new BasicClientCookie("usgmtbgeo", "US");
    cookie2.setDomain(".www.walmart.com");
    cookie2.setPath("/");
    this.cookies.add(cookie2);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONArray skus = UnitedstatesWalmartCrawlerUtils.sanitizeINITIALSTATEJson(doc);

    if (skus.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


      for (Object sku : skus) {
        JSONObject skuJson = (JSONObject) sku;
        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        JSONObject rating = new JSONObject();
        if (skuJson.has(UnitedstatesWalmartCrawlerUtils.RATING)) {
          rating = skuJson.getJSONObject(UnitedstatesWalmartCrawlerUtils.RATING);
        }

        Integer ratingCount = getTotalNumOfRatings(rating);

        ratingReviews.setTotalRating(ratingCount);
        ratingReviews.setTotalWrittenReviews(ratingCount);
        ratingReviews.setAverageOverallRating(getRatingAverage(rating));
        ratingReviews.setInternalId(crawlInternalId(skuJson));
        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.INTERNAL_ID)) {
      internalId = skuJson.getString(UnitedstatesWalmartCrawlerUtils.INTERNAL_ID);
    }

    return internalId;
  }

  private Integer getTotalNumOfRatings(JSONObject ratingJson) {
    Integer totalRating = 0;

    if (ratingJson.has(UnitedstatesWalmartCrawlerUtils.RATING_COUNT)) {
      totalRating = ratingJson.getInt(UnitedstatesWalmartCrawlerUtils.RATING_COUNT);
    }

    return totalRating;
  }

  private Double getRatingAverage(JSONObject ratingJson) {
    Double average = 0d;

    if (ratingJson.has(UnitedstatesWalmartCrawlerUtils.RATING_COUNT)) {
      average = CrawlerUtils.getDoubleValueFromJSON(ratingJson, UnitedstatesWalmartCrawlerUtils.RATING_AVERAGE);
    }

    return average;
  }
}
