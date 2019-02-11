package br.com.lett.crawlernode.crawlers.ratingandreviews.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.FalabellaRatingReviewCrawler;

public class PeruFalabellaRatingReviewCrawler extends FalabellaRatingReviewCrawler {
  public PeruFalabellaRatingReviewCrawler(Session session) {
    super(session);

    super.setApiKey("t6cq31k112riuu8rgp51fq5al");
  }
}
