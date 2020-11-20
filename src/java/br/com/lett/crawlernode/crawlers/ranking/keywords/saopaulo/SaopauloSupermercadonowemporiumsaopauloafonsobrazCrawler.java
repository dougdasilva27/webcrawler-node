package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowemporiumsaopauloafonsobrazCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowemporiumsaopauloafonsobrazCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "emporium-sao-paulo-vila-nova-afonso-braz";
   }
}
