package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourmarketcolegialesCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Elcano 3380 -  C1426 CABA -  Argentina";
   public static final String STORE_ID = "117514";

   public ArgentinaRappicarrefourmarketcolegialesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}