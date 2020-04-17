package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilMartinsmondelezCrawler extends BrasilMartinsCrawler {

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
