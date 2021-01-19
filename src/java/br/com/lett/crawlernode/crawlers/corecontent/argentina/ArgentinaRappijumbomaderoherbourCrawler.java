package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappijumbomaderoherbourCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Avenida Avellaneda 551 2nd Floor, C1405 CNF, Buenos Aires, Argentina";
   public static final String STORE_ID = "113536";

   public ArgentinaRappijumbomaderoherbourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
