package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class RiodejaneiroSupermercadonowlojasamericanaslaranjeirasCrawler extends SupermercadonowCrawlerRanking {

   public RiodejaneiroSupermercadonowlojasamericanaslaranjeirasCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-laranjeiras";
   }
}
