package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappijumbomaderoherbourCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Lola   Mora 450 -  C1107 CABA -  Argentina";
   public static final String STORE_ID = "-";

   public ArgentinaRappijumbomaderoherbourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}