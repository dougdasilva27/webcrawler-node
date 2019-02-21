package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AdidasCrawler;

public class UnitedstatesAdidasCrawler extends AdidasCrawler {
  private static String HOME_PAGE = "https://www.adidas.com";

  public UnitedstatesAdidasCrawler(Session session) {
    super(session, HOME_PAGE);
  }

}
