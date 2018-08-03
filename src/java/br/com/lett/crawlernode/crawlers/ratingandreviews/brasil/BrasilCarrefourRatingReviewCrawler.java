package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class BrasilCarrefourRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilCarrefourRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = crawlRatingReviews(document);
      ratingReviews.setInternalId(crawlInternalId(document));

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(String url) {
    if ((url.contains("/p/")))
      return true;
    return false;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("#productCod").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("value").trim();
    }

    return internalId;
  }

  private RatingsReviews crawlRatingReviews(Document document) {
    RatingsReviews ratingReviews = new RatingsReviews();

    Map<String, Integer> ratingDistribution = crawlRatingDistribution(document);

    ratingReviews.setDate(session.getDate());
    ratingReviews.setTotalRating(computeTotalReviewsCount(ratingDistribution));
    ratingReviews.setAverageOverallRating(crawlAverageOverallRating(document));

    return ratingReviews;
  }

  private Integer computeTotalReviewsCount(Map<String, Integer> ratingDistribution) {
    Integer totalReviewsCount = 0;

    for (String rating : ratingDistribution.keySet()) {
      if (ratingDistribution.get(rating) != null)
        totalReviewsCount += ratingDistribution.get(rating);
    }

    return totalReviewsCount;
  }

  private Double crawlAverageOverallRating(Document document) {
    Double avgOverallRating = null;

    Element avgOverallRatingElement =
        document.select(".sust-review-container .block-review-pagination-bar div.block-rating div.rating.js-ratingCalc").first();
    if (avgOverallRatingElement != null) {
      String dataRatingText = avgOverallRatingElement.attr("data-rating").trim();
      try {
        JSONObject dataRating = new JSONObject(dataRatingText);
        if (dataRating.has("rating")) {
          avgOverallRating = dataRating.getDouble("rating");
        }
      } catch (JSONException e) {
        Logging.printLogError(logger, session, "Error converting String to JSONObject");
      }
    }

    return avgOverallRating;
  }

  private Map<String, Integer> crawlRatingDistribution(Document document) {
    Map<String, Integer> ratingDistributionMap = new HashMap<String, Integer>();

    Elements ratingLineElements = document.select("div.tab-review ul.block-list-starbar li");
    for (Element ratingLine : ratingLineElements) {
      Element ratingStarElement = ratingLine.select("div").first();
      Element ratingStarCount = ratingLine.select("div").last();

      if (ratingStarElement != null && ratingStarCount != null) {
        String ratingStarText = ratingStarElement.text();
        String ratingCountText = ratingStarCount.attr("data-star");

        List<String> parsedNumbers = MathUtils.parseNumbers(ratingStarText);
        if (parsedNumbers.size() > 0 && !ratingCountText.isEmpty()) {
          ratingDistributionMap.put(parsedNumbers.get(0), Integer.parseInt(ratingCountText));
        }
      }
    }

    return ratingDistributionMap;
  }



}
