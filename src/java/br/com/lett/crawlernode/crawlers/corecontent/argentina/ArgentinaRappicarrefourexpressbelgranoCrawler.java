package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourexpressbelgranoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Olazábal 1898 -  C1428 CABA -  Argentina";
   public static final String STORE_ID = "130396";

   public ArgentinaRappicarrefourexpressbelgranoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}