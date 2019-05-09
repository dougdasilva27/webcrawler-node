package br.com.lett.crawlernode.crawlers.ratingandreviews.unitedstates;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.NikeRatingReviewCrawler;

public class UnitedstatesNikeRatingReviewCrawler extends NikeRatingReviewCrawler {

  protected static final String HOME_PAGE = "https://www.nike.com";
  protected static final String COUNTRY_URL = "/us/en_us/";

  public UnitedstatesNikeRatingReviewCrawler(Session session) {
    super(session);
    super.HOME_PAGE = HOME_PAGE;
    super.COUNTRY_URL = COUNTRY_URL;
  }
}
