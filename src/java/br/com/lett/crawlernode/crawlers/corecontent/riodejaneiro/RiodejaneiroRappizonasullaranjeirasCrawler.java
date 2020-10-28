package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class RiodejaneiroRappizonasullaranjeirasCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "zonasul_express";
   private static final String LOCATION = "lat=-22.9338297&lng=-43.1806187";

   public RiodejaneiroRappizonasullaranjeirasCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return STORE_TYPE;
   }

   @Override
   protected String setLocationParameters() {
      return LOCATION;
   }
}
