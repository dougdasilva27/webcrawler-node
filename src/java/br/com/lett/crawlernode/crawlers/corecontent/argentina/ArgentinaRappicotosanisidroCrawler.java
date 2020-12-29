package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicotosanisidroCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av. del   Libertador 18213 -  B1643 Victoria -  Provincia de Buenos Aires -  Argentina";
   public static final String STORE_ID = "132238";

   public ArgentinaRappicotosanisidroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}