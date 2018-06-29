package br.com.lett.crawlernode.crawlers.ratingandreviews.argentina;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 27/06/18
 * 
 * @author gabriel
 *
 */
public class ArgentinaLaanonimaonlineRatingReviewCrawler extends RatingReviewCrawler {

  public ArgentinaLaanonimaonlineRatingReviewCrawler(Session session) {
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
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("#id_item").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
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

    Element one = doc.select(".cuerpo_valoracion span[title=\"No recomendado\"].activa").first();
    Element two = doc.select(".cuerpo_valoracion span[title=\"Malo\"].activa").first();
    Element tree = doc.select(".cuerpo_valoracion span[title=\"Regular\"].activa").first();
    Element four = doc.select(".cuerpo_valoracion span[title=\"Bueno\"].activa").first();
    Element five = doc.select(".cuerpo_valoracion span[title=\"Muy Bueno\"].activa").first();


    if (five != null) {
      avgRating = 5d;
    } else if (four != null) {
      avgRating = 4d;
    } else if (tree != null) {
      avgRating = 3d;
    } else if (two != null) {
      avgRating = 2d;
    } else if (one != null) {
      avgRating = 1d;
    }

    return avgRating;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    return doc.select("#ver_comentarios > div.comentario").size();
  }


  private boolean isProductPage(Document doc) {
    return doc.select("#id_item").first() != null;
  }

}
