package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoOldRappiCrawlerRanking;

public class MexicoRappisuperamaciudadmexicoCrawler extends MexicoOldRappiCrawlerRanking {

   public static final String STORE_ID = "1306702047";

   public MexicoRappisuperamaciudadmexicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
