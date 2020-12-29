package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappiveacordobaCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Pcia de   Córdoba 649 -  San Miguel de Tucumán -  Tucumán";
   public static final String STORE_ID = "127714";

   public ArgentinaRappiveacordobaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}