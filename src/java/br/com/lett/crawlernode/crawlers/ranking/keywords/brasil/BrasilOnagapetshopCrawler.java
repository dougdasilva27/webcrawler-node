package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.TrayCommerceCrawler;

public class BrasilOnagapetshopCrawler extends TrayCommerceCrawler {

  public BrasilOnagapetshopCrawler(Session session) {
    super(session);
  }

  @Override
  protected String setStoreId() {
    return "758683";
  }

  @Override
  protected String setHomePage() {
    return "https://www.petsonaga.com.br/";
  }
}
