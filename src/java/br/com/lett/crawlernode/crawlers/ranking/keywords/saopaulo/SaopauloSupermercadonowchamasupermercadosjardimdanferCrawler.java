package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowchamasupermercadosjardimdanferCrawler extends SupermercadonowCrawlerRanking {


   public SaopauloSupermercadonowchamasupermercadosjardimdanferCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-jardim-danfer";
   }
}
