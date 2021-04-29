package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class SaopauloRappiatacadaoCrawler extends BrasilRappiCrawlerRanking {

   public SaopauloRappiatacadaoCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   public static final String STORE_ID = "900136949";
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
