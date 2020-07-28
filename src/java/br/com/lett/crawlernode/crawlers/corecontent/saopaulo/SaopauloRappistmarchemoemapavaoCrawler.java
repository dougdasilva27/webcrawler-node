package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchemoemapavaoCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "st_marche";
   private static final String LOCATION = "lng=-46.6646497&lat=-23.6022758";

   public SaopauloRappistmarchemoemapavaoCrawler(Session session) {
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
