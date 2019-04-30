package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;

public class RiodejaneiroDrogariavenancioRatingReviewCrawler extends RatingReviewCrawler {

  public RiodejaneiroDrogariavenancioRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      TrustvoxRatingCrawler r = new TrustvoxRatingCrawler(session, "105530", logger);
      ratingReviewsCollection = r.extractRatingAndReviewsForVtex(doc, dataFetcher);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }
}
