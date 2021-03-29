package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoOldRappiCrawlerRanking;

public class MexicoRappiwalmartguadalajaraCrawler extends MexicoOldRappiCrawlerRanking {

   public static final String STORE_ID = "990006043";

   public MexicoRappiwalmartguadalajaraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
