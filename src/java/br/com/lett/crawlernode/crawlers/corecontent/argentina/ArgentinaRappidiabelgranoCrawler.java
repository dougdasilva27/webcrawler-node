package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiabelgranoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Olazábal 4832 -  C1431 CABA -  Argentina";
   public static final String STORE_ID = "135341";

   public ArgentinaRappidiabelgranoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}