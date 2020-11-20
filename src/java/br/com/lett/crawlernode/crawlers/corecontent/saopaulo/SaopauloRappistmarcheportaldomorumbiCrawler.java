package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappistmarcheportaldomorumbiCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lng=-46.7360631&lat=-23.6163906";
   public static final String STORE_ID = "900033773";

   public SaopauloRappistmarcheportaldomorumbiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
