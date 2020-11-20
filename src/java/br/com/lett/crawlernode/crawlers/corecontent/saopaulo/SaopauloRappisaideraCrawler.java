package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappisaideraCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "special_liquor";
   private static final String LOCATION = "lng=-46.6888804&lat=-23.561535";
   public static final String STORE_ID = "900006685";

   public SaopauloRappisaideraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
