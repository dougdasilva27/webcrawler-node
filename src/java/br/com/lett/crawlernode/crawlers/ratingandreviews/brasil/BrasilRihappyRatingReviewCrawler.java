package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;

public class BrasilRihappyRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilRihappyRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      ratingReviewsCollection = new TrustvoxRatingCrawler(session, "76552", logger).extractRatingAndReviewsForVtex(document, dataFetcher);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

}
