package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

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
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class BrasilBemolRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilBemolRatingReviewCrawler(Session session) {
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
        Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
        Double avgRating = getTotalAvgRating(document);

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

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input[name=ProductID]");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  /**
   * Average is calculate Example: 5 estrelas [percentage bar] 347 4 estrelas [percentage bar] 42
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(docRating, ".wd-product-rating .rating-average", null, true, ',', session);

    if (avgRating == null) {
      avgRating = 0d;
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
    Integer totalRating = 0;
    Elements rating = doc.select(".wd-product-rating [itemprop=reviewCount]");

    if (rating != null) {
      String votes = rating.attr("content").replaceAll("[^0-9]", "").trim();

      if (!votes.isEmpty()) {
        totalRating += Integer.parseInt(votes);
      }
    }

    return totalRating;
  }


  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=ProductID]").isEmpty();
  }

}
