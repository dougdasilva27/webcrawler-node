package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappiCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "";
   private static final String LOCATION = "lat=-23.584&lng=-46.671";
   public static final String STORE_ID = "700001704";

   public SaopauloRappiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
