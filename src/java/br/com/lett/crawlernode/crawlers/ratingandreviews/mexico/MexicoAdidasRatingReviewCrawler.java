package br.com.lett.crawlernode.crawlers.ratingandreviews.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.AdidasRatingReviewCrawler;

public class MexicoAdidasRatingReviewCrawler extends AdidasRatingReviewCrawler {
  private static String HOME_PAGE = "https://www.adidas.mx";

  public MexicoAdidasRatingReviewCrawler(Session session) {
    super(session, HOME_PAGE);
  }

}
