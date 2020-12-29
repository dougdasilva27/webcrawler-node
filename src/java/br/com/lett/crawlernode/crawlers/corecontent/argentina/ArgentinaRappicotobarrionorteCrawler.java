package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicotobarrionorteCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Charcas   2980 -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "132231";

   public ArgentinaRappicotobarrionorteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}