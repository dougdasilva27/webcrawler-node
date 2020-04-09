package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MartinsCrawler;

public class BrasilMartinsmondelezCrawler extends MartinsCrawler {

  public BrasilMartinsmondelezCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getPassword() {
    return "monica08";
  }

  @Override
  protected String getLogin() {
    return "erika.rosa@mdlz.com";
  }
}
