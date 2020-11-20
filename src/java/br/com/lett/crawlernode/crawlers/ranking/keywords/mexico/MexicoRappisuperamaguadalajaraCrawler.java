package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoRappiCrawlerRanking;

public class MexicoRappisuperamaguadalajaraCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "990007292";

   public MexicoRappisuperamaguadalajaraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
