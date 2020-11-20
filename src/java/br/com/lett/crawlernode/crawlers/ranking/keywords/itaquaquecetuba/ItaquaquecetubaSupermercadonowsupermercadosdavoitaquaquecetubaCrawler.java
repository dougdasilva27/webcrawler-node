package br.com.lett.crawlernode.crawlers.ranking.keywords.itaquaquecetuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class ItaquaquecetubaSupermercadonowsupermercadosdavoitaquaquecetubaCrawler extends SupermercadonowCrawlerRanking {

   public ItaquaquecetubaSupermercadonowsupermercadosdavoitaquaquecetubaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-itaquaquecetuba";
   }
}
