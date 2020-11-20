package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrayCommerceCrawler;

public class BrasilOnagapetshopCrawler extends TrayCommerceCrawler {

  public BrasilOnagapetshopCrawler(Session session) {
    super(session);
  }

  @Override
  protected String setSellerName() {
    return "Pets Onaga";
  }
}
