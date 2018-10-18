package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

/**
 * Date: 18/10/18
 * 
 * @author gabriel
 *
 */
public class BrasilCissamagazineRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilCissamagazineRatingReviewCrawler(Session session) {
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

      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("#prod-codigo");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  /**
   * Avg appear in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    Element avg = doc.selectFirst(".avaliacao-geral-topo span [itemprop=ratingValue]");
    if (avg != null) {
      avgRating = MathUtils.parseDoubleWithDot(avg.attr("content"));

      if (avgRating == null) {
        avgRating = 0d;
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
    Integer rating = 0;

    Element reviewCount = doc.selectFirst(".avaliacao-geral-topo [itemprop=reviewCount]");
    if (reviewCount != null) {
      String text = reviewCount.attr("content").replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        rating = Integer.parseInt(text);
      }
    }

    return rating;
  }


  private boolean isProductPage(Document doc) {
    return !doc.select("#prod-codigo").isEmpty();
  }

}
