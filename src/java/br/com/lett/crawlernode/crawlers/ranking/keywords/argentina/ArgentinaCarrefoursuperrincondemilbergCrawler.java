package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ArgentinaCarrefoursuper;

public class ArgentinaCarrefoursuperrincondemilbergCrawler extends ArgentinaCarrefoursuper {

  public ArgentinaCarrefoursuperrincondemilbergCrawler(Session session) {
    super(session);
  }

  private static final String CEP = "1648";

  @Override
  protected String getCep() {
    return CEP;
  }
}
