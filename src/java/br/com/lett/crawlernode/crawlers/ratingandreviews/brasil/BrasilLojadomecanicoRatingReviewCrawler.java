package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilLojadomecanicoRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilLojadomecanicoRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = scrapInternalId(doc);
      Integer totalNumOfEvaluations = 0;
      Double avgRating = 0.0;

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, true, false);
      if (json.has("aggregateRating")) {
        json = json.getJSONObject("aggregateRating");

        totalNumOfEvaluations = scrapNumOfEval(json);
        avgRating = scrapAvgRating(json);
      }

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-page") != null;
  }

  private String scrapInternalId(Document doc) {
    String internalId = null;
    Element internalIdElement = doc.selectFirst("meta[itemprop=sku]");

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("content").toString().trim();
    }

    return internalId;
  }

  private Integer scrapNumOfEval(JSONObject json) {
    Integer numOfEval = 0;

    if (json.has("reviewCount")) {
      numOfEval = json.getInt("reviewCount");
    }

    return numOfEval;
  }

  private Double scrapAvgRating(JSONObject json) {
    Double abgRating = 0.0;

    if (json.has("ratingValue")) {
      abgRating = json.getDouble("ratingValue");
    }

    return abgRating;
  }
}
