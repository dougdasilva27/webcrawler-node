
package br.com.lett.crawlernode.crawlers.corecontent.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ComperCrawler;

public class BrasiliaComperCrawler extends ComperCrawler {

   private static final String STORE_ID = "6688";
   private static final String MULTI_STORE_ID = "00040015110010000000100000000000";

   public BrasiliaComperCrawler(Session session) {
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
