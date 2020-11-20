package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class RiodejaneiroSupermercadonowhortifrutiflamengoCrawler extends SupermercadonowCrawlerRanking {

   public RiodejaneiroSupermercadonowhortifrutiflamengoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "hortifruti-flamengo";
   }
}
