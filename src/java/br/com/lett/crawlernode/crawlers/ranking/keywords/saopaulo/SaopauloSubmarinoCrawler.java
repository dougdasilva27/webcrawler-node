package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.B2WCrawler;

public class SaopauloSubmarinoCrawler extends B2WCrawler {

  public SaopauloSubmarinoCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreName() {
    return "submarino";
  }
}
