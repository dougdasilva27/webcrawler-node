package br.com.lett.crawlernode.crawlers.ranking.keywords.maceio;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.LeroymerlinCrawler;

public class MaceioLeroymerlinCrawler extends LeroymerlinCrawler {

  public MaceioLeroymerlinCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    this.region = "alagoas";
  }
}
