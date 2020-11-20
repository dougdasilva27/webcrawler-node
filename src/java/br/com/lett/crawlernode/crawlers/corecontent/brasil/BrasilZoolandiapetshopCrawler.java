package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrayCommerceCrawler;

public class BrasilZoolandiapetshopCrawler extends TrayCommerceCrawler {

  @Override
  protected String setSellerName() {
    return "Zoolandia petshop";
  }

  public BrasilZoolandiapetshopCrawler(Session session) {
    super(session);
  }
}
