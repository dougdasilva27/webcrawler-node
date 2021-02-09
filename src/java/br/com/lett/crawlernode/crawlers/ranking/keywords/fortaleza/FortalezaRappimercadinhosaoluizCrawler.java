package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class FortalezaRappimercadinhosaoluizCrawler extends BrasilRappiCrawlerRanking {
   public FortalezaRappimercadinhosaoluizCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900022515";
   public static final String STORE_TYPE = "hiper";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return STORE_TYPE;
   }

}

