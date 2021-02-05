package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class BrasilRappigbarbosaaracajuCrawler extends BrasilRappiCrawlerRanking {
   public BrasilRappigbarbosaaracajuCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900053930";
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

