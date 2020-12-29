package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourmarketretiroCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Maipú 940 -  B1638 Vicente López -  Provincia de Buenos Aires -  Argentina";
   public static final String STORE_ID = "118276";

   public ArgentinaRappicarrefourmarketretiroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}