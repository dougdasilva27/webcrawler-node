
package br.com.lett.crawlernode.crawlers.ranking.keywords.cuiaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ComperCrawlerRanking;

public class CuiabaComperCrawler extends ComperCrawlerRanking {

   private static final String STORE_ID = "6637";

   public CuiabaComperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
