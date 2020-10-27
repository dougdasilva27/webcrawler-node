package br.com.lett.crawlernode.crawlers.ranking.keywords.campogrande;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.ComperCrawlerRanking;

public class CampograndeComperCrawler extends ComperCrawlerRanking {

   private static final String STORE_ID = "2";
   private static final String STORE_UF = "MS";

   public CampograndeComperCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreUf() {
      return STORE_UF;
   }
}
