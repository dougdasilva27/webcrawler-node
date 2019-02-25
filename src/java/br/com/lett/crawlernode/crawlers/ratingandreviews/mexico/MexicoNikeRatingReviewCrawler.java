package br.com.lett.crawlernode.crawlers.ratingandreviews.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.NikeRatingReviewCrawler;

public class MexicoNikeRatingReviewCrawler extends NikeRatingReviewCrawler {

  protected static final String HOME_PAGE = "https://www.nike.com/mx/";
  protected static final String COUNTRY_URL = "/es_la/";

  public MexicoNikeRatingReviewCrawler(Session session) {
    super(session);
  }
}
