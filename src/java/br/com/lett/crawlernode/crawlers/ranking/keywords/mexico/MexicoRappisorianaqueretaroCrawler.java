package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoOldRappiCrawlerRanking;

public class MexicoRappisorianaqueretaroCrawler extends MexicoOldRappiCrawlerRanking {

   public static final String STORE_ID = "160194978";

   public MexicoRappisorianaqueretaroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
