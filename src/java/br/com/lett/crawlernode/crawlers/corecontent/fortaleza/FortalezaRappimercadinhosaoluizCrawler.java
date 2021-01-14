package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class FortalezaRappimercadinhosaoluizCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "sao_luiz";
   private static final String LOCATION = "lat=-3.799424000000&lng=-38.503115000000";
   public static final String STORE_ID = "900022515";

   public FortalezaRappimercadinhosaoluizCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return STORE_TYPE;
   }

   @Override
   protected String setLocationParameters() {
      return LOCATION;
   }

}
