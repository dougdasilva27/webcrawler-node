package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappidrogaraiaCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "raia";
   private static final String LOCATION = "lat=-23.5420705&lng=-46.6371135";
   private static final String STORE_ID = "900004068";

   public SaopauloRappidrogaraiaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
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
