
package br.com.lett.crawlernode.crawlers.corecontent.campogrande;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ComperCrawler;

public class CampograndeComperCrawler extends ComperCrawler {

   private static final String STORE_ID = "6602";
   private static final String MULTI_STORE_ID = "02010714110000101010000010001000";

   public CampograndeComperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getMultiStoreId() {
      return MULTI_STORE_ID;
   }
}
