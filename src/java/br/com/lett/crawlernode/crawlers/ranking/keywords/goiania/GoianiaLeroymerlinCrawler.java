package br.com.lett.crawlernode.crawlers.ranking.keywords.goiania;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.LeroymerlinCrawler;

public class GoianiaLeroymerlinCrawler extends LeroymerlinCrawler {

  public GoianiaLeroymerlinCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    this.region = "goiania";
  }
}
