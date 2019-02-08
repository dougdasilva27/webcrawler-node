package br.com.lett.crawlernode.crawlers.ratingandreviews.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.FalabellaRatingReviewCrawler;

public class ArgentinaFalabellaRatingReviewCrawler extends FalabellaRatingReviewCrawler {
  public ArgentinaFalabellaRatingReviewCrawler(Session session) {
    super(session);

    super.setApiKey("u5y9xkb4b1ly36cs5uvppykl0");
  }
}
