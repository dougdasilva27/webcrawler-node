package br.com.lett.crawlernode.crawlers.ranking.keywords.osasco;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class OsascoSupermercadonowsupermercadoslopesjdcipavaCrawler extends SupermercadonowCrawlerRanking {

   public OsascoSupermercadonowsupermercadoslopesjdcipavaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-jd-cipava";
   }
}
