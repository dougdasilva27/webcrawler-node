package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.LeroymerlinCrawler;

public class RiodejaneiroLeroymerlinCrawler extends LeroymerlinCrawler {

  public RiodejaneiroLeroymerlinCrawler(Session session) {
    super(session);
  }

  @Override
  protected void processBeforeFetch() {
    this.region = "rio_de_janeiro";
  }
}
