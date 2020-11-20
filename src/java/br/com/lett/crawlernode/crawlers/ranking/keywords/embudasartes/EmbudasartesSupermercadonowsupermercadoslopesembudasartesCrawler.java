package br.com.lett.crawlernode.crawlers.ranking.keywords.embudasartes;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class EmbudasartesSupermercadonowsupermercadoslopesembudasartesCrawler extends SupermercadonowCrawlerRanking {

   public EmbudasartesSupermercadonowsupermercadoslopesembudasartesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-embu";
   }
}
