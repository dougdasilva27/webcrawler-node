package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 25/05/18
 * 
 * @author gabriel
 *
 */
public class BrasilBelezanawebRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilBelezanawebRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = CrawlerUtils.scrapStringSimpleInfo(document, ".product-sku", true);

      if (internalId != null) {
        Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
        Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  /**
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc, Integer ratingCount) {
    Double avgRating = 0d;

    if (ratingCount > 0) {
      Element avg = doc.selectFirst(".reviews-header .rating-value-container");

      if (avg != null) {
        String text = avg.ownText().replaceAll("[^0-9.]", "").replace(",", ".").trim();

        if (!text.isEmpty()) {
          avgRating = Double.parseDouble(text);
        }
      }
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in html
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document docRating) {
    Integer totalRating = 0;
    Element totalRatingElement = docRating.selectFirst(".reviews-header .rating-count");

    if (totalRatingElement != null) {
      String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        totalRating = Integer.parseInt(text);
      }
    }

    return totalRating;
  }


  private boolean isProductPage(Document doc) {
    return !doc.select(".product-sku").isEmpty();
  }
}
