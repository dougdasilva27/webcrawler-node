package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.LeroymerlinCrawler;

public class FlorianopolisLeroymerlinCrawler extends LeroymerlinCrawler {

  public FlorianopolisLeroymerlinCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    this.region = "santa_catarina";
  }
}
