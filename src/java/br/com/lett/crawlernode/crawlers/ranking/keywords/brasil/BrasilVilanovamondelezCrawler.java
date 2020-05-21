package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilVilanova;

public class BrasilVilanovamondelezCrawler extends BrasilVilanova {

  public BrasilVilanovamondelezCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getCnpj() {
    return "33033028004090";
  }

  @Override
  protected String getPassword() {
    return "681543";
  }
}
