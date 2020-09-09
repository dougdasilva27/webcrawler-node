package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.B2WCrawlerRanking;

public class SaopauloAmericanasCrawler extends B2WCrawlerRanking {

  public SaopauloAmericanasCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreName() {
    return "americanas";
  }

}
