package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilMartinskraftheinzCrawler extends BrasilMartinsCrawler {

  public BrasilMartinskraftheinzCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getPassword() {
    return "heinz2020";
  }

  @Override
  protected String getLogin() {
    return "lorenzo.lamas@kraftheinz.com";
  }
}
