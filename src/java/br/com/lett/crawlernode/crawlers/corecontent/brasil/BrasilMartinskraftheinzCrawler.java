package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MartinsCrawler;

public class BrasilMartinskraftheinzCrawler extends MartinsCrawler {

  public BrasilMartinskraftheinzCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getPassword() {
    return "24373852";
  }

  @Override
  protected String getLogin() {
    return "lorenzo.lamas@kraftheinz.com";
  }
}
