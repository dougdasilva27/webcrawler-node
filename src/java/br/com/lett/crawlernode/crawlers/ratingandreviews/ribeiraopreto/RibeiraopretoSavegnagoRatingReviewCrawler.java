package br.com.lett.crawlernode.crawlers.ratingandreviews.ribeiraopreto;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import models.AdvancedRatingReview;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 */
public class RibeiraopretoSavegnagoRatingReviewCrawler extends RatingReviewCrawler {

  public RibeiraopretoSavegnagoRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);

      YourreviewsRatingCrawler yr = new YourreviewsRatingCrawler(session, cookies, logger, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);
      Document docRating = yr.crawlPageRatingsFromYourViews(internalId, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);

      Integer totalNumOfEvaluations = yr.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yr.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalId);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setInternalId(internalId);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element elementInternalId = doc.select("meta[itemprop=\"productID\"]").first();
    if (elementInternalId != null) {
      internalId = elementInternalId.attr("content").trim();
    }

    return internalId;
  }

  private boolean isProductPage(Document document) {
    return document.select("#___rc-p-sku-ids").first() != null;
  }
}
