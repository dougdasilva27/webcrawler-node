
package br.com.lett.crawlernode.crawlers.corecontent.campogrande;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ComperCrawler;

public class CampograndeComperCrawler extends ComperCrawler {

  private static final String STORE_ID = "2";

  public CampograndeComperCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreId() {
    return STORE_ID;
  }
}
