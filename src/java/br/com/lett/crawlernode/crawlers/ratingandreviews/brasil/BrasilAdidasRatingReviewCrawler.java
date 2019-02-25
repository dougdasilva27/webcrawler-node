package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.AdidasRatingReviewCrawler;

public class BrasilAdidasRatingReviewCrawler extends AdidasRatingReviewCrawler {
  private static String HOME_PAGE = "https://www.adidas.com.br";

  public BrasilAdidasRatingReviewCrawler(Session session) {
    super(session, HOME_PAGE);
  }

}
