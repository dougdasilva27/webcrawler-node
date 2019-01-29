package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.B2WCrawler;

public class SaopauloAmericanasCrawler extends B2WCrawler {

  public SaopauloAmericanasCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreName() {
    return "americanas";
  }

}
