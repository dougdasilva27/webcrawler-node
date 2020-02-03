package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import models.RatingsReviews;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BrasilMultisomRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilMultisomRatingReviewCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalId = crawlInternalId(document);

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalId, "e5006da2-cf44-417c-a86c-d99fbfc7fb23", dataFetcher);

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
    return document.select(".detailProduct").first() != null;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("input[name=data[Produto][rating][id_produto]]").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("value").trim();
    }

    return internalId;
  }
}

