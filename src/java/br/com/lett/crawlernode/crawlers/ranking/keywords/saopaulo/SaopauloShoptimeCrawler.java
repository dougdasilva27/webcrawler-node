package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.B2WCrawler;

public class SaopauloShoptimeCrawler extends B2WCrawler {

  public SaopauloShoptimeCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreName() {
    return "shoptime";
  }
}
