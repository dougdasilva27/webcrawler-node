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
public class BrasilMercadolivrecotyRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilMercadolivrecotyRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=itemId]", "value");

      if (internalPid != null) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=variation]", "value");
        internalId = internalId == null ? internalPid : internalPid + "-" + internalId;

        Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
        Double avgRating = getTotalAvgRating(doc);

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
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    Element avg = doc.selectFirst(".review-summary-average");
    if (avg != null) {
      String text = avg.ownText().replaceAll("[^0-9.]", "");

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
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
    Element totalRatingElement = docRating.selectFirst(".core-review .average-legend");

    if (totalRatingElement != null) {
      String text = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        totalRating = Integer.parseInt(text);
      }
    }

    return totalRating;
  }


  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=itemId]").isEmpty();
  }
}
