package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowemporiumsaopaulojuremaCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowemporiumsaopaulojuremaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "emporium-sao-paulo-moema-jurema";
   }
}
