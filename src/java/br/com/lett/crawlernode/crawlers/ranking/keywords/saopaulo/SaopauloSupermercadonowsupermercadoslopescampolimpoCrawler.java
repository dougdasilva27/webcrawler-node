package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowsupermercadoslopescampolimpoCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowsupermercadoslopescampolimpoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-campo-limpo";
   }
}
