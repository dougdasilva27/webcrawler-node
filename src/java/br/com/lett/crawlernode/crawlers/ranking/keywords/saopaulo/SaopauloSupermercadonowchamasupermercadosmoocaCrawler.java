package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowchamasupermercadosmoocaCrawler extends SupermercadonowCrawlerRanking {


   public SaopauloSupermercadonowchamasupermercadosmoocaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-mooca";
   }
}
