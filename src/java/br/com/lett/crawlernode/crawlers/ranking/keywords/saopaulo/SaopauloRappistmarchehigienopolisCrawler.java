package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class SaopauloRappistmarchehigienopolisCrawler extends BrasilRappiCrawlerRanking {

   public SaopauloRappistmarchehigienopolisCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return "900130015";
   }
}
