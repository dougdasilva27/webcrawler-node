package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WCrawlerRanking;

public class SaopauloShoptimeCrawler extends B2WCrawlerRanking {

  public SaopauloShoptimeCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreName() {
    return "shoptime";
  }
}
