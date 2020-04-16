package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoRappiCrawlerRanking;

public class MexicoRappilacomersateliteCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "942000254";

   public MexicoRappilacomersateliteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
