package br.com.lett.crawlernode.crawlers.ranking.keywords.mogidascruzes;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class MogidascruzesSupermercadonowsupermercadosdavomogidascruzesCrawler extends SupermercadonowCrawlerRanking {

   public MogidascruzesSupermercadonowsupermercadosdavomogidascruzesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-mogi-das-cruzes";
   }
}
