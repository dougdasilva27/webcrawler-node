package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class RiodejaneiroRappizonasullaranjeirasCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "zonasul_express";
   private static final String LOCATION = "lat=-22.9338297&lng=-43.1806187";
   public static final String STORE_ID = "900141025";

   public RiodejaneiroRappizonasullaranjeirasCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
