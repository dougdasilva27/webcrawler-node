package br.com.lett.crawlernode.crawlers.ratingandreviews.unitedstates;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.AdidasRatingReviewCrawler;

public class UnitedstatesAdidasRatingReviewCrawler extends AdidasRatingReviewCrawler {
  private static String HOME_PAGE = "https://www.adidas.com";

  public UnitedstatesAdidasRatingReviewCrawler(Session session) {
    super(session, HOME_PAGE);
    // TODO Auto-generated constructor stub
  }

}
