package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappicarrefourCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "carrefour";
   private static final String LOCATION = "lat=-23.584&lng=-46.671";
   private static final String STORE_ID = "900020401";

   public SaopauloRappicarrefourCrawler(Session session) {
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
