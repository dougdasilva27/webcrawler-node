package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilMartinsmondelezCrawler extends BrasilMartinsCrawler {

  public BrasilMartinsmondelezCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getPassword() {
    return "luz3001";
  }

  @Override
  protected String getLogin() {
    return "patriciaf3001@gmail.com";
  }
}
