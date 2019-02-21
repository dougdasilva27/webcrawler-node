package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AdidasCrawler;

public class MexicoAdidasCrawler extends AdidasCrawler {
  public static String HOME_PAGE = "https://www.adidas.mx";

  public MexicoAdidasCrawler(Session session) {
    super(session, HOME_PAGE);
  }

}
