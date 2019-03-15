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
public class BrasilCentauroRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilCentauroRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject chaordicJson = crawlChaordicJson(doc);

      ratingReviews.setTotalRating(getTotalNumOfRatings(chaordicJson));
      ratingReviews.setAverageOverallRating(getTotalAvgRating(chaordicJson));

      List<String> idList = crawlIdList(chaordicJson);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private List<String> crawlIdList(JSONObject chaordicJson) {
    List<String> internalIds = new ArrayList<>();

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
   * Number of ratings appear in html element
   * 
   * @param doc
   * @return
   */
  private Integer getTotalNumOfRatings(JSONObject chaordicJson) {
    Integer totalRating = 0;

    if (chaordicJson.has("details")) {
      JSONObject details = chaordicJson.getJSONObject("details");

      if (details.has("reviews")) {
        String text = details.get("reviews").toString().replaceAll("[^0-9]", "");

        if (!text.isEmpty()) {
          totalRating = Integer.parseInt(text);
        }
      }
    }

    return totalRating;
  }

  /**
   * Average is calculate Example: 5 estrelas [percentage bar] 347 4 estrelas [percentage bar] 42
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(JSONObject chaordicJson) {
    Double avgRating = 0d;

    if (chaordicJson.has("details")) {
      JSONObject details = chaordicJson.getJSONObject("details");

      if (details.has("rate")) {
        String text = details.get("rate").toString().replaceAll("[^0-9.]", "");

        if (!text.isEmpty()) {
          avgRating = Double.parseDouble(text);
        }
      }
    }

    return avgRating;
  }

  private JSONObject crawlChaordicJson(Document doc) {
    JSONObject skuJson = new JSONObject();

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();


      if (script.contains("window.chaordic_meta = ")) {
        String token = "window.chaordic_meta = ";
        int x = script.indexOf(token) + token.length();
        int y = script.lastIndexOf(';');

        String json = script.substring(x, y);

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            JSONObject chaordic = new JSONObject(json);

            if (chaordic.has("product")) {
              skuJson = chaordic.getJSONObject("product");
            }
          } catch (Exception e1) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return skuJson;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-item").first() != null;
  }

}
