package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicotovicentelópezCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Maipú 1758 -  Vicente López -  Provincia de Buenos Aires -  Argentina";
   public static final String STORE_ID = "114974";

   public ArgentinaRappicotovicentelópezCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}