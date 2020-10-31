package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchehigienopolisCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lng=-46.6588414&lat=-23.5422447";
   private static final String STORE_ID = "900020365";

   public SaopauloRappistmarchehigienopolisCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return STORE_TYPE;
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }

   @Override
   protected String setLocationParameters() {
      return LOCATION;
   }
}
