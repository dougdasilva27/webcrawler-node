package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.B2WRatingReviewCrawler;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloShoptimeRatingReviewCrawler extends B2WRatingReviewCrawler {

  private static final String HOME_PAGE = "https://www.shoptime.com.br/";

  public SaopauloShoptimeRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
    super.homePage = HOME_PAGE;
  }
}
