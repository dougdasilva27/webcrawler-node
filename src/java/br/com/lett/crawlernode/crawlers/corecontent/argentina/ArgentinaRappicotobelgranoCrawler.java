package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicotobelgranoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Cabildo 545 -  C1426 CABA -  Argentina";
   public static final String STORE_ID = "124171";

   public ArgentinaRappicotobelgranoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}