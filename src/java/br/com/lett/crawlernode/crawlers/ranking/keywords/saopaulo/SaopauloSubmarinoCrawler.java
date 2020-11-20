package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WCrawlerRanking;

public class SaopauloSubmarinoCrawler extends B2WCrawlerRanking {

  public SaopauloSubmarinoCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreName() {
    return "submarino";
  }
}
