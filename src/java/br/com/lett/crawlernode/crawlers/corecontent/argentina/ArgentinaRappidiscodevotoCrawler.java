package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscodevotoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Francisco Beiró 3560 -  C1419 CABA -  Argentina";
   public static final String STORE_ID = "114610";

   public ArgentinaRappidiscodevotoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}