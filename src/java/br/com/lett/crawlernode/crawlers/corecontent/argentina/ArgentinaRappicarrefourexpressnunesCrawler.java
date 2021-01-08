package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourexpressnunesCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Deheza   2239, C1429 CABA, Argentina";
   public static final String STORE_ID = "130394";

   public ArgentinaRappicarrefourexpressnunesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
