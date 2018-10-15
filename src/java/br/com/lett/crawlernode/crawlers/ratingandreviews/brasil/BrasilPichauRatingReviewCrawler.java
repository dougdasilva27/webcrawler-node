package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 15/10/18
 * 
 * @author gabriel
 *
 */
public class BrasilPichauRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilPichauRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);
      JSONObject productInfo = crawlProductInfo(document);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(productInfo);
      Double avgRating = getTotalAvgRating(productInfo);

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

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input[name=product]");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  private Double getTotalAvgRating(JSONObject ratingJson) {
    Double avgRating = 0d;

    if (ratingJson.has("ratingValue") && ratingJson.get("ratingValue") instanceof Double) {
      avgRating = ratingJson.getDouble("ratingValue");
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatings(JSONObject ratingJson) {
    Integer totalRating = 0;

    if (ratingJson.has("ratingCount")) {
      String text = ratingJson.get("ratingCount").toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        totalRating = Integer.parseInt(text);
      }
    }

    return totalRating;
  }

  private JSONObject crawlProductInfo(Document doc) {
    JSONObject obj = new JSONObject();

    Elements scripts = doc.select("script");
    for (Element e : scripts) {
      String script = e.html().replace(" ", "");

      if (script.contains("aggregateRating")) {
        JSONArray array = CrawlerUtils.stringToJsonArray(script);
        if (array.length() > 0) {
          obj = array.getJSONObject(0);
        }
        break;
      }
    }

    if (obj.has("aggregateRating")) {
      return obj.getJSONObject("aggregateRating");
    }

    return obj;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-details").isEmpty();
  }
}
