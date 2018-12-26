package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.LeroymerlinCrawler;

public class BelohorizonteLeroymerlinCrawler extends LeroymerlinCrawler {

  public BelohorizonteLeroymerlinCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    this.region = "belo_horizonte";
  }
}
