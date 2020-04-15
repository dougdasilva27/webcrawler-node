package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoRappiCrawlerRanking;

public class MexicorappichedrauimeridaCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "1306705764";

   public MexicorappichedrauimeridaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
