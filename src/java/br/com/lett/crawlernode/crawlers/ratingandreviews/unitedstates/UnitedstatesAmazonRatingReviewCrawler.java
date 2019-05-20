package br.com.lett.crawlernode.crawlers.ratingandreviews.unitedstates;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 22/06/18
 * 
 * @author gabriel
 *
 */
public class UnitedstatesAmazonRatingReviewCrawler extends RatingReviewCrawler {

  public UnitedstatesAmazonRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.APACHE);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(document,
          "#acrCustomerReviewText, #reviews-medley-cmps-expand-head > #dp-cmps-expand-header-last > span:not([class])", true, 0);
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

    Element internalIdElement = doc.select("input[name^=ASIN]").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  /**
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;
    Element reviews =
        doc.select("#reviewsMedley .arp-rating-out-of-text, #reviews-medley-cmps-expand-head > #dp-cmps-expand-header-last span.a-icon-alt").first();

    if (reviews != null) {
      String text = reviews.ownText().trim();

      if (text.contains("of")) {
        String avgText = text.split("of")[0].replaceAll("[^0-9.]", "").trim();

        if (!avgText.isEmpty()) {
          avgRating = Double.parseDouble(avgText);
        }
      }
    }

    return avgRating;
  }

  private boolean isProductPage(Document doc) {
    return doc.select("#dp").first() != null;
  }

}
