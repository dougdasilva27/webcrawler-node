package br.com.lett.crawlernode.crawlers.ranking.keywords.guarulhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class GuarulhosSupermercadonowsupermercadoslopesmacedoCrawler extends SupermercadonowCrawlerRanking {

   public GuarulhosSupermercadonowsupermercadoslopesmacedoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-macedo";
   }
}
