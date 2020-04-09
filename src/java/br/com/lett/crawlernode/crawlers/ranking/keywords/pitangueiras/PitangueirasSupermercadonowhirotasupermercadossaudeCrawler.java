package br.com.lett.crawlernode.crawlers.ranking.keywords.pitangueiras;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class PitangueirasSupermercadonowhirotasupermercadossaudeCrawler extends SupermercadonowCrawlerRanking {

   public PitangueirasSupermercadonowhirotasupermercadossaudeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-saude-jabaquara";
   }
}
