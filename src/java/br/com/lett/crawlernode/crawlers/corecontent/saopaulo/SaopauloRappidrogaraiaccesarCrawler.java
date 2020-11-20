package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappidrogaraiaccesarCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "raia";
   private static final String LOCATION = "lng=-46.6725578&lat=-23.5594218";
   public static final String STORE_ID = "900004148";

   public SaopauloRappidrogaraiaccesarCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
