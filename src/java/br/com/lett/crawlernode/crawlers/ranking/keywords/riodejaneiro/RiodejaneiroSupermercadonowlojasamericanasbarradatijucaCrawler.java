package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class RiodejaneiroSupermercadonowlojasamericanasbarradatijucaCrawler extends SupermercadonowCrawlerRanking {

   public RiodejaneiroSupermercadonowlojasamericanasbarradatijucaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-barra-da-tijuca";
   }
}
