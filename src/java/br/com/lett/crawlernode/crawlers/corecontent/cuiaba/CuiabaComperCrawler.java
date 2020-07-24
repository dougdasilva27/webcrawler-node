
package br.com.lett.crawlernode.crawlers.corecontent.cuiaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ComperCrawler;

public class CuiabaComperCrawler extends ComperCrawler {

  private static final String STORE_ID = "1";

  public CuiabaComperCrawler(Session session) {
    super(session);
  }

  @Override
  protected String getStoreId() {
    return STORE_ID;
  }
}
