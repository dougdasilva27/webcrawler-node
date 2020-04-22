package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowlojanestlechucrizaidanCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowlojanestlechucrizaidanCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "loja-nestle-chucri-zaidan";
   }
}
