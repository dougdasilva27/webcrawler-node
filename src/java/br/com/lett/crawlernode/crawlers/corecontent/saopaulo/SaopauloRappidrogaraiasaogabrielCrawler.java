package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappidrogaraiasaogabrielCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "raia";
   private static final String LOCATION = "lng=-46.7076327&lat=-23.5537518";
   public static final String STORE_ID = "900130103";

   public SaopauloRappidrogaraiasaogabrielCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
