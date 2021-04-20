package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoOldRappiCrawlerRanking;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoRappiCrawlerRanking;

public class MexicoRappihebmonterreyCrawler extends MexicoRappiCrawlerRanking {

   public static final String STORE_ID = "990004993";

   public MexicoRappihebmonterreyCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return "heb";
   }
}
