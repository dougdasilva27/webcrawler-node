package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappigigaCrawler extends BrasilRappiCrawler {
  private static final String STORE_TYPE = "giga";
  private static final String LOCATION = "lat=-23.5078641&lng=-46.68407819999999";
  private static final String STORE_ID = "900022260";

  public SaopauloRappigigaCrawler(Session session) {
    super(session);
  }

  @Override
  protected String setStoreType() {
    return STORE_TYPE;
  }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }

  @Override
  protected String setLocationParameters() {
    return LOCATION;
  }
}
