package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappijumbonordeltaCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av. de   los Lagos 6660 -  B1670 Tigre -  Provincia de Buenos Aires -  Argentina";
   public static final String STORE_ID = "117170";

   public ArgentinaRappijumbonordeltaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}