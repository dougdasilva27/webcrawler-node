package br.com.lett.crawlernode.crawlers.corecontent.recife;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;


public class RecifeRappiatacadaoCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "atacadao";
   private static final String LOCATION = "lng=-34.9462256&lat=-8.0296839";
   public static final String STORE_ID = "900159163";

   public RecifeRappiatacadaoCrawler(Session session) {
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
