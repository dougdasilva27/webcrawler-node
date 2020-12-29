package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscoparaguayCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Paraguay   4302 -  C1425 BSL -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "279";

   public ArgentinaRappidiscoparaguayCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}