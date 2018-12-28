package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilNutricaototalRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilNutricaototalRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = getInternalId(doc, ".product-essential .no-display input[name=product]");
      Integer totalNumOfEvaluations = crawlNumOfEvaluations(doc, "meta[itemprop=ratingCount]");
      Double avgRating = crawlAvgRating(doc, "meta[itemprop=ratingValue]");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-essential") != null;
  }

  private String getInternalId(Document doc, String selector) {
    Element e = doc.selectFirst(selector);
    String internalId = null;

    if (e != null) {
      internalId = e.attr("value");
    }

    return internalId;
  }

  private Integer crawlNumOfEvaluations(Document doc, String selector) {
    Element e = doc.selectFirst(selector);

    if (e != null) {
      String aux = e.attr("content");

      if (!aux.isEmpty()) {
        return Integer.parseInt(aux);
      }
    }

    return 0;
  }

  private Double crawlAvgRating(Document doc, String selector) {
    Element e = doc.selectFirst(selector);

    if (e != null) {
      String aux = e.attr("content");

      if (!aux.isEmpty()) {
        return Double.parseDouble(aux);
      }
    }

    return 0.0;
  }
}
