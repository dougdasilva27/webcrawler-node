package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscovelezarsfieldCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av. Vélez Sarsfield 1845 X5016 Córdoba, Cordoba, Argentina";
   public static final String STORE_ID = "111279";

   public ArgentinaRappidiscovelezarsfieldCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
