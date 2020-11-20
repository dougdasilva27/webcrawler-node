package br.com.lett.crawlernode.crawlers.ranking.keywords.guarulhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class GuarulhosSupermercadonowsupermercadoslopesjdtranquilidadeCrawler extends SupermercadonowCrawlerRanking {

   public GuarulhosSupermercadonowsupermercadoslopesjdtranquilidadeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-jd-tranquilidade";
   }
}
