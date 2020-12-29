package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiaavellanedaCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Calle   32 1300 25 -  25 de Mayo -  B1870 Avellaneda -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "-";

   public ArgentinaRappidiaavellanedaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}