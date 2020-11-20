package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowmastersupermercadosconsolacaoCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowmastersupermercadosconsolacaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "master-supermercados-consolacao";
   }
}
