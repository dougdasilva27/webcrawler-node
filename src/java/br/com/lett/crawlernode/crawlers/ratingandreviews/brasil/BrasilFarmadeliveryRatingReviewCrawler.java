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
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 */
public class BrasilFarmadeliveryRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilFarmadeliveryRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document, session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);

      if (internalId != null) {
        Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
        Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

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

    Element elementID = doc.select("input[name=product]").first();
    if (elementID != null) {
      internalId = Integer.toString(Integer.parseInt(elementID.val()));
    }

    return internalId;
  }

  /**
   * Average is in html element Example: Número de Avaliações : 24 Média das notas : 4.6 /5
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc, Integer ratingCount) {
    Double avgRating = 0d;

    if (ratingCount > 0) {
      Element avg = doc.select(".rating-box-product .rating-box .rating").first();

      if (avg != null) {
        Double percentage = MathUtils.normalizeTwoDecimalPlaces(Double.parseDouble(avg.attr("style").replaceAll("[^0-9.]", "").trim()));

        if (percentage != null) {
          avgRating = MathUtils.normalizeTwoDecimalPlaces(5 * (percentage / 100d));
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
    Element totalRatingElement = docRating.select(".rating-box-product .amount a").first();

    if (totalRatingElement != null) {
      String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        totalRating = Integer.parseInt(text);
      }
    }

    return totalRating;
  }


  private boolean isProductPage(Document document, String url) {
    Element elementProduct = document.select("div.product-view").first();
    return elementProduct != null && !url.contains("/review/");
  }

}
