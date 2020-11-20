package br.com.lett.crawlernode.crawlers.ranking.keywords.suzano;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SuzanoSupermercadonowsupermercadosdavosuzanoCrawler extends SupermercadonowCrawlerRanking {

   public SuzanoSupermercadonowsupermercadosdavosuzanoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-suzano";
   }
}
