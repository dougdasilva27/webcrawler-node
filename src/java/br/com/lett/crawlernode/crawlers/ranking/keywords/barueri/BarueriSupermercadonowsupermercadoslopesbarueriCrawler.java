package br.com.lett.crawlernode.crawlers.ranking.keywords.barueri;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class BarueriSupermercadonowsupermercadoslopesbarueriCrawler extends SupermercadonowCrawlerRanking {

   public BarueriSupermercadonowsupermercadoslopesbarueriCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-barueri";
   }
}
