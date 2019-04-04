package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.VtexRatingCrawler;


/**
 * Date: 27/09/18
 * 
 * @author gabriel
 *
 */
public class BrasilLojacotyRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilLojacotyRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      VtexRatingCrawler vtex = new VtexRatingCrawler(session, "https://www.lojacoty.com.br/", logger, cookies);
      ratingReviewsCollection = vtex.extractRatingAndReviewsForVtex(document, dataFetcher);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

}
