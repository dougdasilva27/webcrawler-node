package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappijumboacoyteCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av.   Acoyte 702 -  C1405 BGS -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "113536";

   public ArgentinaRappijumboacoyteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}