package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MartinsKeywords;

public class BrasilMartinsmondelezCrawler extends MartinsKeywords {

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
