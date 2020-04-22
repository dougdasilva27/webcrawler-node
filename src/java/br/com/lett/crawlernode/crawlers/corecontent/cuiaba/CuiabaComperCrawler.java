
package br.com.lett.crawlernode.crawlers.corecontent.cuiaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ComperCrawler;

public class CuiabaComperCrawler extends ComperCrawler {

   private static final String STORE_ID = "6637";
   private static final String MULTI_STORE_ID = "05050714110000101010100010100010";

   public CuiabaComperCrawler(Session session) {
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
