package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscoramosmejíaCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Rivadavia 14452 -  B1704 Ramos Mejía -  Provincia de Buenos Aires -  Argentina";
   public static final String STORE_ID = "115380";

   public ArgentinaRappidiscoramosmejíaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}