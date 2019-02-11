package br.com.lett.crawlernode.crawlers.ratingandreviews.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.FalabellaRatingReviewCrawler;

public class ColombiaFalabellaRatingReviewCrawler extends FalabellaRatingReviewCrawler {
  public ColombiaFalabellaRatingReviewCrawler(Session session) {
    super(session);

    super.setApiKey("oub11ocqqjr1ukjf43c9dukc9");
  }
}
