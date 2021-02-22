package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class SaopauloRappidrogaraiajardimpaulistaCrawler extends BrasilRappiCrawlerRanking {

   public SaopauloRappidrogaraiajardimpaulistaCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900004148";
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
