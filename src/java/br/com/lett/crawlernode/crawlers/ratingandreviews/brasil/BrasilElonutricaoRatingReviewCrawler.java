package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class BrasilElonutricaoRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilElonutricaoRatingReviewCrawler(Session session) {
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
        Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select("#prod_padd").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("#ProdutoId").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  /**
   * 
   * @param document
   * @param ratingCount
   * @return
   */
  private Double getTotalAvgRating(Document doc, Integer ratingCount) {
    Double avgRating = 0d;

    if (ratingCount > 0) {
      Element avg = doc.select(".prod_box_avaliacao_bg_geral[style]").first();

      if (avg != null) {
        Double percentage = MathUtils
            .normalizeTwoDecimalPlaces(Double.parseDouble(CommonMethods.getLast(avg.attr("style").split(";")).replaceAll("[^0-9.]", "").trim()));

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
    Element totalRatingElement = docRating.select(".prod_avaliacao_txt").first();

    if (totalRatingElement != null) {
      String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        totalRating = Integer.parseInt(text);
      }
    }

    return totalRating;
  }
}
