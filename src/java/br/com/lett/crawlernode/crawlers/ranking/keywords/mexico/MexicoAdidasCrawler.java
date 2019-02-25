package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.AdidasCrawler;

public class MexicoAdidasCrawler extends AdidasCrawler {
  private static String HOST = "www.adidas.mx";

  public MexicoAdidasCrawler(Session session) {
    super(session, HOST);
  }

}
