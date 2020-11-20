package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowlojasamericanaslapaCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowlojasamericanaslapaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-lapa";
   }
}
