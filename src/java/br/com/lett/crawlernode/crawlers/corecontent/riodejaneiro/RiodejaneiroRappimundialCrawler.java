package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class RiodejaneiroRappimundialCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "mundial";
   private static final String LOCATION = "lat=-22.952&lng=-43.192";
   private static final String STORE_ID = "900020828";

   public RiodejaneiroRappimundialCrawler(Session session) {
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
