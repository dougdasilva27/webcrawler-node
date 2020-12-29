package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourexpressbalvaneraCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Gral.   Urquiza 648 -  C1221 ADD -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "130409";

   public ArgentinaRappicarrefourexpressbalvaneraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}