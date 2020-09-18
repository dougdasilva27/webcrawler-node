package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappisaideraCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "special-liquor";
   private static final String LOCATION = "lng=-46.6888804&lat=-23.561535";

   public SaopauloRappisaideraCrawler(Session session) {
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
