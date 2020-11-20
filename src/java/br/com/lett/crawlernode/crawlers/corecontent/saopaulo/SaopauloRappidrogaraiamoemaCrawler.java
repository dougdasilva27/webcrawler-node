package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappidrogaraiamoemaCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "raia";
   private static final String LOCATION = "lng=-46.6574083&lat=-23.6054293";
   public static final String STORE_ID = "900004068";

   public SaopauloRappidrogaraiamoemaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
