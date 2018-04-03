package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
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
 * Date: 04/04/2018
 * 
 * @author gabriel
 *
 */
public class BrasilPetloveRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilPetloveRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      List<String> idList = crawlIdList(doc);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private List<String> crawlIdList(Document doc) {
    List<String> internalIds = new ArrayList<>();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer.push(", ");", false);
    JSONObject productJson = json.has("info") ? json.getJSONObject("info") : new JSONObject();
    JSONArray arraySkus = productJson.has("variants") ? productJson.getJSONArray("variants") : new JSONArray();

    for (int i = 0; i < arraySkus.length(); i++) {
      internalIds.add(crawlInternalId(arraySkus.getJSONObject(i)));
    }

    return internalIds;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku").trim();
    }

    return internalId;
  }

  /**
   * Number of ratings appear in html element
   * 
   * @param doc
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    Integer totalRating = 0;
    Element rating = doc.select(".box-rating [itemprop=reviewCount]").first();

    if (rating != null) {
      String votes = rating.attr("content");

      if (!votes.isEmpty()) {
        totalRating = Integer.parseInt(votes);
      }
    }

    return totalRating;
  }

  /**
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.select(".rating-avg").first();

    if (rating != null) {
      String text = rating.ownText();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

  private boolean isProductPage(Document doc) {
    return doc.select("#product").first() != null;
  }

}
