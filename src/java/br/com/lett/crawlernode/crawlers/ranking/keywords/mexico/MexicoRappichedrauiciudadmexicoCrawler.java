package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoRappiCrawlerRanking;

public class MexicoRappichedrauiciudadmexicoCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "990002972";

   public MexicoRappichedrauiciudadmexicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }


}
