package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

/**
 * Date: 28/07/17
 * 
 * @author gabriel
 *
 */
public class BrasilSaraivaRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilSaraivaRatingReviewCrawler(Session session) {
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

      if (internalId != null) {
        Request request = RequestBuilder.create()
            .setUrl("https://saraiva.mais.social/reviews/transit/get/products/srv/" + internalId + "/reviews/offuser?data=VvV=").setCookies(cookies)
            .build();
        JSONObject ratingJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
        Integer totalNumOfEvaluations = getTotalNumOfRatings(ratingJson);
        Double avgRating = getTotalAvgRating(ratingJson);

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element elementSpan = doc.select("section.product-info h1 span").first();
    if (elementSpan != null) {
      String spanText = elementSpan.text();
      List<String> parsedNumbers = MathUtils.parseNumbers(spanText);
      if (!parsedNumbers.isEmpty()) {
        internalId = parsedNumbers.get(0);
      }
    }

    return internalId;
  }

  /**
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(JSONObject ratingJson) {
    Double avgRating = 0.0;

    if (ratingJson.has("aggregateRating")) {
      JSONObject aggregate = ratingJson.getJSONObject("aggregateRating");

      if (aggregate.has("ratingValue")) {
        avgRating = aggregate.getDouble("ratingValue");
      }
    }

    return avgRating;
  }

  /**
   * @param ratingJson
   * @return
   */
  private Integer getTotalNumOfRatings(JSONObject ratingJson) {
    Integer totalRating = 0;

    if (ratingJson.has("aggregateRating")) {
      JSONObject aggregate = ratingJson.getJSONObject("aggregateRating");

      if (aggregate.has("ratingComposition")) {
        JSONObject values = aggregate.getJSONObject("ratingComposition");

        for (int i = 1; i <= values.length(); i++) {
          totalRating += values.getInt(Integer.toString(i));
        }
      }
    }

    return totalRating;
  }


  private boolean isProductPage(Document document) {
    Element elementProduct = document.select("section.product-allinfo").first();
    return elementProduct != null;
  }

}
