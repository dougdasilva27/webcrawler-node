package br.com.lett.crawlernode.crawlers.ratingandreviews.mexico;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 22/06/18
 * 
 * @author gabriel
 *
 */
public class MexicoAmazonRatingReviewCrawler extends RatingReviewCrawler {

  public MexicoAmazonRatingReviewCrawler(Session session) {
    super(session);
  }

  private AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

  @Override
  protected Document fetch() {
    return amazonScraperUtils.fetchProductPage(cookies, dataFetcher);
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
   * Avg appear in html element
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

      if (text.contains("de")) {
        String avgText = text.split("de")[0].replaceAll("[^0-9.]", "").trim();

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
