package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoRappiCrawlerRanking;

public class MexicorappisuperamaciudadmexicoCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "1306702047";

   public MexicorappisuperamaciudadmexicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
