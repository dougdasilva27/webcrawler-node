package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class SaopauloOnofreRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloOnofreRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      String internalId = crawlInternalId(document);

      ratingReviewsCollection.addRatingReviews(crawlRating(document, internalId));
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }


    return ratingReviewsCollection;
  }

  private RatingsReviews crawlRating(Document doc, String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();

    YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

    Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "c8cbeadc-e277-4c51-b84b-e19b6ef9c063");
    Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
    Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setDate(session.getDate());
    ratingReviews.setInternalId(internalId);

    return ratingReviews;
  }


  private boolean isProductPage(Document doc) {
    return doc.select("#skuId").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element elementInternalPid = doc.select("#skuId").first();
    if (elementInternalPid != null) {
      internalId = elementInternalPid.text();
    }

    return internalId;
  }
}
