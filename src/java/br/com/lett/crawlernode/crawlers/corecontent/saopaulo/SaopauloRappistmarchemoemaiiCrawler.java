package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchemoemaiiCrawler extends BrasilRappiCrawler {

   private static final String STORE_ID = "900037057";

   public SaopauloRappistmarchemoemaiiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return "st_marche";
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }

   @Override
   protected String setLocationParameters() {
      return "lat=-23.6022758&lng=-46.6646497";
   }
}
