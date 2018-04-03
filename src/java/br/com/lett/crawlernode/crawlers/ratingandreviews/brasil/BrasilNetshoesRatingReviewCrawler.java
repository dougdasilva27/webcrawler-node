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
 * Date: 04/04/2018
 * 
 * @author gabriel
 *
 */
public class BrasilNetshoesRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilNetshoesRatingReviewCrawler(Session session) {
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

    JSONObject chaordicJson = crawlChaordicJson(doc);
    JSONArray arraySkus = chaordicJson != null && chaordicJson.has("skus") ? chaordicJson.getJSONArray("skus") : new JSONArray();

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
   * Average is calculate Example: 5 estrelas [percentage bar] 347 4 estrelas [percentage bar] 42
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.select(".rating [itemprop=ratingValue]").first();

    if (rating != null) {
      String text = rating.text().trim();

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
    Integer totalRating = 0;
    Element rating = doc.select(".rating [itemprop=reviewCount]").first();

    if (rating != null) {
      String votes = rating.attr("content");

      if (!votes.isEmpty()) {
        totalRating = Integer.parseInt(votes);
      }
    }

    return totalRating;
  }

  private JSONObject crawlChaordicJson(Document doc) {
    JSONObject skuJson = new JSONObject();

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();


      if (script.contains("freedom.metadata.chaordic(")) {
        String token = "loader.js', '";
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf("');", x);

        String json = script.substring(x, y);

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            JSONObject chaordic = new JSONObject(json);

            if (chaordic.has("product")) {
              skuJson = chaordic.getJSONObject("product");
            }
          } catch (Exception e1) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return skuJson;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".reference").first() != null;
  }

}
