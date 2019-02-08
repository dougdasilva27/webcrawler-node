package br.com.lett.crawlernode.crawlers.ratingandreviews.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.FalabellaRatingReviewCrawler;


public class ChileFalabellaRatingReviewCrawler extends FalabellaRatingReviewCrawler {
  public ChileFalabellaRatingReviewCrawler(Session session) {
    super(session);

    super.setApiKey("mk9fosfh4vxv20y8u5pcbwipl");
  }
}
