package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoCornershopCrawlerRanking;

public class SaopauloCornershopcarrefourCrawler extends MexicoCornershopCrawlerRanking {

   public SaopauloCornershopcarrefourCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return "6555";
   }

   @Override
   protected String getProductStoreId() {
      return "6555";
   }
}
