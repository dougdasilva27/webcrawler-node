package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappistmarchemoemajauaperiCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lng=-46.6649493&lat=-23.602749";
   public static final String STORE_ID = "900037057";

   public SaopauloRappistmarchemoemajauaperiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
