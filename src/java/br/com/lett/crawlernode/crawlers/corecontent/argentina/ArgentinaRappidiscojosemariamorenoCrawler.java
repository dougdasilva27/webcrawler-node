package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscojosemariamorenoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av. José   María Moreno 362 -  C1424 AAQ -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "-";

   public ArgentinaRappidiscojosemariamorenoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}