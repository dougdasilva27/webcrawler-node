package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscosancristobalCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Entre Ríos 361 -  C1079 ABD -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "113910";

   public ArgentinaRappidiscosancristobalCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}