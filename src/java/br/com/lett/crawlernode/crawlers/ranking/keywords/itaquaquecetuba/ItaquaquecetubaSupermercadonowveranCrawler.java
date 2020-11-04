package br.com.lett.crawlernode.crawlers.ranking.keywords.itaquaquecetuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class ItaquaquecetubaSupermercadonowveranCrawler extends SupermercadonowCrawlerRanking {

   public ItaquaquecetubaSupermercadonowveranCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "veran-supermercados-itaquaquecetuba";
   }
}
