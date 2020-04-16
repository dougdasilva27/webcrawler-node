package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoRappiCrawlerRanking;

public class MexicoRappiwalmartsateliteCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "990006044";

   public MexicoRappiwalmartsateliteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
