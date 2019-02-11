package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import models.RatingsReviews;

public class BrasilIbyteRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilIbyteRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalId = crawlInternalPid(document);

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "92581cf1-146e-48cd-853a-1873e0e3fee1");

      Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);

      Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }


  private boolean isProductPage(Document document) {
    Element elementProduct = document.select(".view-sku").first();
    return (elementProduct != null);
  }


  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element elementPid = document.select("input[name=product]").first();
    if (elementPid != null) {
      internalPid = elementPid.attr("value").trim();
    }
    return internalPid;
  }
}


