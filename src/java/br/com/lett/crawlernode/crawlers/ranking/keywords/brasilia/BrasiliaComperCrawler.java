
package br.com.lett.crawlernode.crawlers.ranking.keywords.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ComperCrawlerRanking;

public class BrasiliaComperCrawler extends ComperCrawlerRanking {

   private static final String STORE_ID = "6688";

   public BrasiliaComperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
