package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MartinsKeywords;

public class BrasilMartinskraftheinzCrawler extends MartinsKeywords {

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
