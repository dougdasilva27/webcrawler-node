package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoRappiCrawlerRanking;

public class MexicorappilacomerciudadmexicoCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "130670648";

   public MexicorappilacomerciudadmexicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
