package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchemorumbiCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lat=-23.616816&lng=-46.7353953";
   public static final String STORE_ID = "900033773";

   public SaopauloRappistmarchemorumbiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
