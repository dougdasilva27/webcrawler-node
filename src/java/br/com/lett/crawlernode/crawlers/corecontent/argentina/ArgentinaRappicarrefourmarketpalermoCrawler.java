package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourmarketpalermoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Raúl   Scalabrini Ortiz 3128 -  C1425 CABA -  Argentina";
   public static final String STORE_ID = "117515";

   public ArgentinaRappicarrefourmarketpalermoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}