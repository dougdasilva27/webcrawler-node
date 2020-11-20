package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowchamasupermercadosviladalilaCrawler extends SupermercadonowCrawlerRanking {


   public SaopauloSupermercadonowchamasupermercadosviladalilaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-vila-dalila";
   }
}
