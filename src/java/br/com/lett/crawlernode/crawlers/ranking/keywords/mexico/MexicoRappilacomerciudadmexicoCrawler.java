package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoRappiCrawlerRanking;

public class MexicoRappilacomerciudadmexicoCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "1306706486";

   public MexicoRappilacomerciudadmexicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
