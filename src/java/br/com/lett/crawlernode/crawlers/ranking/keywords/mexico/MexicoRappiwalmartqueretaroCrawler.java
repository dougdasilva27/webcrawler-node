package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoRappiCrawlerRanking;

public class MexicoRappiwalmartqueretaroCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "990006048";

   public MexicoRappiwalmartqueretaroCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return "walmart";
   }
}
