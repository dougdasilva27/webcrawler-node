package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarcheitaimCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lat=-23.5797453&lng=-46.6702749";
   public static final String STORE_ID = "900020365";

   public SaopauloRappistmarcheitaimCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
