package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;

/**
 * Date: 17/09/18
 * 
 * @author gabriel
 *
 */
public class BrasilKitchenaidRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilKitchenaidRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    return new TrustvoxRatingCrawler(session, "105322", logger).extractRatingAndReviewsForVtex(document, dataFetcher);
  }
}
