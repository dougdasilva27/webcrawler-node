package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoCornershopCrawlerRanking;

public class RiodejaneiroCornershopcarrefourCrawler extends MexicoCornershopCrawlerRanking {

   public RiodejaneiroCornershopcarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return "7484";
   }

   @Override
   protected String getProductStoreId() {
      return "1327";
   }
}
