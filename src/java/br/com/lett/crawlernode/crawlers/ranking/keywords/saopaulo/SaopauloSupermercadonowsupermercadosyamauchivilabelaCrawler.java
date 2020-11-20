package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowsupermercadosyamauchivilabelaCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowsupermercadosyamauchivilabelaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-yamauchi-vila-bela";
   }
}
