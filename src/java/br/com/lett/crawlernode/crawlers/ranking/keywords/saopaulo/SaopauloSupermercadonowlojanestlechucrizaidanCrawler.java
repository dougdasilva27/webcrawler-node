package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowlojanestlechucrizaidanCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowlojanestlechucrizaidanCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "loja-nestle-chucri-zaidan";
   }

   @Override
   protected String getHost() {
      return "www.emporionestle.com.br";
   }
}
