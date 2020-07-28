package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchemoemajauaperiCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lng=-46.6649493&lat=-23.602749";

   public SaopauloRappistmarchemoemajauaperiCrawler(Session session) {
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
