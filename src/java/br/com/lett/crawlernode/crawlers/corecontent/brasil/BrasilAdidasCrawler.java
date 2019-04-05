package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AdidasCrawler;

public class BrasilAdidasCrawler extends AdidasCrawler {
  private static String HOME_PAGE = "https://www.adidas.com.br";

  public BrasilAdidasCrawler(Session session) {
    super(session, HOME_PAGE);
  }

}
