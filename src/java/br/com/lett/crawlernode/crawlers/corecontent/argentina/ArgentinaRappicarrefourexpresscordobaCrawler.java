package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourexpresscordobaCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Independencia   855 -  X5000 IUQ -  Córdoba -  Argentina";
   public static final String STORE_ID = "12692";

   public ArgentinaRappicarrefourexpresscordobaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}