package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrayCommerceCrawler;

public class BrasilAnimalshowstoreCrawler extends TrayCommerceCrawler {

  @Override
  protected String setSellerName() {
    return "Animal show store";
  }

  public BrasilAnimalshowstoreCrawler(Session session) {
    super(session);
  }
}
