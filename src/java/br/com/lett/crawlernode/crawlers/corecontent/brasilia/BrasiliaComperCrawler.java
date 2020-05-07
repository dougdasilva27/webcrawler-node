
package br.com.lett.crawlernode.crawlers.corecontent.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ComperCrawler;

public class BrasiliaComperCrawler extends ComperCrawler {

   private static final String STORE_ID = "3";

   public BrasiliaComperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
