package br.com.lett.crawlernode.crawlers.ranking.keywords.sorocaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SorocabaSupermercadonowsupermercadoslopesmoinhoCrawler extends SupermercadonowCrawlerRanking {

   public SorocabaSupermercadonowsupermercadoslopesmoinhoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-sorocaba-moinho";
   }
}
