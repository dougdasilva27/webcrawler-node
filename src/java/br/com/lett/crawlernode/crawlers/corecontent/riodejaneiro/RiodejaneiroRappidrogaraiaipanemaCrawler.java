package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class RiodejaneiroRappidrogaraiaipanemaCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "raia";
   private static final String LOCATION = "lng=-43.2049749&lat=-22.9842173";
   public static final String STORE_ID = "900005268";

   public RiodejaneiroRappidrogaraiaipanemaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
