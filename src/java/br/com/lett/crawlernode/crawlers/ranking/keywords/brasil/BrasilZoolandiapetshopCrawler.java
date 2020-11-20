package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TrayCommerceCrawler;

public class BrasilZoolandiapetshopCrawler extends TrayCommerceCrawler {

  @Override
  protected String setStoreId() {
    return "466244";
  }

  public BrasilZoolandiapetshopCrawler(Session session) {
    super(session);
  }

  @Override
  protected String setHomePage() {
    return "https://www.zoolandiapetshop.com.br/";
  }

}
