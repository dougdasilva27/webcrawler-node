package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappijumbopalermoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Av Int   Bullrich 345 -  C1425 CABA -  Provincia de Buenos Aires";
   public static final String STORE_ID = "112241";

   public ArgentinaRappijumbopalermoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}