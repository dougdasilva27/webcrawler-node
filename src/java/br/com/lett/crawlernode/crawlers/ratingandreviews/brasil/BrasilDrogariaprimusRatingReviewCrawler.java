package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.RatingsReviews;

/**
 * Date: 28/08/17
 * 
 * @author gabriel
 *
 */
public class BrasilDrogariaprimusRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilDrogariaprimusRatingReviewCrawler(Session session) {
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
      Pair<Integer, Double> rating = getRating(document);

      Integer totalNumOfEvaluations = rating.getFirst();
      Double avgRating = rating.getSecond();

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;

  }

  /**
   * Crawl the internalId trying two different approaches.
   * 
   * @param doc
   * @return the internalId or null if it wasn't found
   */
  private String crawlInternalId(Document document) {
    // two possible options here
    // #produtoid -> is a simple html tag with the id as a value attribute
    // #productdata -> is a json

    // we try to get the data from the json before, because
    // it's less prone to format changes, as it is probably
    // an API response.
    // In case of any error we try to get the id from the
    // attribute 'value' in the #produtoid tag

    Element productDataElement = document.select("#productdata").first();
    if (productDataElement != null) {
      String jsonText = productDataElement.attr("value");
      if (jsonText != null && !jsonText.isEmpty()) {
        try {
          JSONObject productData = new JSONObject(jsonText);
          return productData.getString("id");
        } catch (JSONException jsonException) {
          Logging.printLogDebug(logger, session, "InternalId error [" + jsonException.getMessage() + "]");
          Logging.printLogDebug(logger, "Trying backup approach ... ");
        }
      }
    }

    Element productIdElement = document.select("#produtoid").first();
    if (productIdElement != null) {
      String attrValue = productIdElement.attr("value");
      if (attrValue == null || attrValue.isEmpty()) {
        Logging.printLogDebug(logger, session, "Backup approach also failed [attrValue = " + attrValue + "]");
      } else {
        return attrValue;
      }
    }

    return null;
  }

  /**
   * Avg is calculated
   * 
   * @param document
   * @return
   */
  private Pair<Integer, Double> getRating(Document doc) {
    Double avgRating = 0d;
    Integer ratingNumber = 0;

    Element ratingCount = doc.selectFirst(".product-rating [itemprop=\"ratingCount\"]");
    if (ratingCount != null) {
      String text = ratingCount.attr("content").replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        ratingNumber = Integer.parseInt(text);
      }
    }

    Element ratingAverage = doc.selectFirst(".product-rating .rating .average");
    if (ratingAverage != null) {
      avgRating = MathUtils.parseDouble(ratingAverage.ownText());
    }

    return new Pair<>(ratingNumber, avgRating);
  }

  private boolean isProductPage(Document doc) {
    return doc.select("div.product-contents").first() != null;
  }

}
