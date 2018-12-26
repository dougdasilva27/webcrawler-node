package br.com.lett.crawlernode.crawlers.ranking.keywords.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.LeroymerlinCrawler;

public class BrasiliaLeroymerlinCrawler extends LeroymerlinCrawler {

  public BrasiliaLeroymerlinCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    this.region = "brasilia";
  }
}
