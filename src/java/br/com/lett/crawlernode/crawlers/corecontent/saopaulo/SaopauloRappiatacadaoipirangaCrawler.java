package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappiatacadaoipirangaCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "atacadao";
   private static final String LOCATION = "lng=-46.6022277&lat=-23.5779717";
   public static final String STORE_ID = "900159165";

   public SaopauloRappiatacadaoipirangaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}

