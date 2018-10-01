package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 *         In time crawler was made, there was no rating on any product in this market
 *
 */
public class BrasilSephoraRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilSephoraRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject chaordicJson = crawlChaordicJson(doc);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(doc));

      List<String> idList = crawlIdList(chaordicJson);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".reference") != null;
  }

  private JSONObject crawlChaordicJson(Document doc) {
    JSONObject skuJson = new JSONObject();

    Elements scripts = doc.select("script[type=\"application/ld+json\"]");

    for (Element e : scripts) {
      String script = e.html().trim();

      if (script.contains("sku") && script.startsWith("[") && script.endsWith("]")) {
        try {
          JSONArray array = new JSONArray(script);

          if (array.length() > 0) {
            skuJson = array.getJSONObject(0);
          }
        } catch (Exception e1) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
        }

        break;
      }
    }

    return skuJson;
  }

  /**
   * Average is calculate
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.selectFirst("#customer-reviews .average span");

    if (rating != null) {
      String text = rating.ownText().replaceAll("[^0-9.]", "").trim();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

  /**
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.selectFirst("#customer-reviews .average");

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }


  private List<String> crawlIdList(JSONObject skuJson) {
    List<String> idList = new ArrayList<>();

    if (skuJson.has("offers")) {
      JSONArray skus = skuJson.getJSONArray("offers");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("sku")) {
          idList.add(Integer.toString(sku.getInt("sku")));
        }
      }
    }

    return idList;
  }
}
