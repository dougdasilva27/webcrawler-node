package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.AdidasCrawler;

public class BrasilAdidasCrawler extends AdidasCrawler {
  public static String HOME_PAGE = "https://www.adidas.com.br";

  public BrasilAdidasCrawler(Session session) {
    super(session, HOME_PAGE);
    // TODO Auto-generated constructor stub
  }

}
