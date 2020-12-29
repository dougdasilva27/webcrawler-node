package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiavicentelopezCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Bartolomé Mitre 4211 -  B1605 Munro -  Provincia de Buenos Aires -  Argentina";
   public static final String STORE_ID = "135511";

   public ArgentinaRappidiavicentelopezCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}