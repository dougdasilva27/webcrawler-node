package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class BrasilRappigbarbosaaracajuCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "gbarbosa";
   private static final String LOCATION = "lat=-10.94164700000&lng=-37.058646000000";
   public static final String STORE_ID = "900053930";

   public BrasilRappigbarbosaaracajuCrawler(Session session) {
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



