package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class BrasilDufrioRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilDufrioRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(Fetcher.WEBDRIVER);
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
      Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("div span[itemprop=mpn]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.text().trim();
    }

    return internalId;
  }

  /**
   * Avg appear in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc, Integer totalRatings) {
    Double avgRating = 0d;

    if (totalRatings != null && totalRatings > 0) {
      Elements ratings = doc.select(".boxBarras .item");

      Integer values = 0;

      for (Element e : ratings) {
        Element stars = e.select(".p1").first();
        Element value = e.select(".p3").first();

        if (stars != null && value != null) {
          Integer star = Integer.parseInt(stars.ownText().replaceAll("[^0-9]", "").trim());
          Integer countStars = Integer.parseInt(value.ownText().replaceAll("[^0-9]", "").trim());

          values += star * countStars;
        }
      }

      avgRating = MathUtils.normalizeTwoDecimalPlaces(((double) values) / totalRatings);
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
    Integer ratingNumber = 0;
    Elements evaluations = doc.select(".boxBarras .p3");

    for (Element e : evaluations) {
      String text = e.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        ratingNumber += Integer.parseInt(text);
      }
    }

    return ratingNumber;
  }


  private boolean isProductPage(Document doc) {
    if (doc.select(".detalheProduto").first() != null) {
      return true;
    }
    return false;
  }

}
