package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.AdidasCrawler;

public class MexicoAdidasCrawler extends AdidasCrawler {
  private static String HOME_PAGE = "https://www.adidas.mx";

  public MexicoAdidasCrawler(Session session) {
    super(session, HOME_PAGE);
  }

}
