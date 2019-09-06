package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ArgentinaCarrefoursuper;

public class ArgentinaCarrefoursuperCrawler extends ArgentinaCarrefoursuper {

  public ArgentinaCarrefoursuperCrawler(Session session) {
    super(session);
  }

  private static final String CEP = "1646";

  @Override
  protected String getCep() {
    return CEP;
  }
}
