package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourrosariopellegriniCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Pellegrini 3250 -  S2000 Rosario -  Santa Fe -  Argentina";
   public static final String STORE_ID = "127499";

   public ArgentinaRappicarrefourrosariopellegriniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}