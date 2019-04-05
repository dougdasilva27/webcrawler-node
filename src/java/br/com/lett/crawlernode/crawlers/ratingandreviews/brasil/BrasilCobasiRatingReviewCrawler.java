package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class BrasilCobasiRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilCobasiRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    TrustvoxRatingCrawler t = new TrustvoxRatingCrawler(session, "105831", logger);
    ratingReviewsCollection = t.extractRatingAndReviewsForVtex(document, dataFetcher);

    return ratingReviewsCollection;
  }
}
