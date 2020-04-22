package br.com.lett.crawlernode.crawlers.ranking.keywords.campogrande;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ComperCrawlerRanking;

public class CampograndeComperCrawler extends ComperCrawlerRanking {

   private static final String STORE_ID = "6602";

   public CampograndeComperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
