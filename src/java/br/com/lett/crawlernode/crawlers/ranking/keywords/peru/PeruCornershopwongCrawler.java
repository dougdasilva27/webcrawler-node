package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoCornershopCrawlerRanking;

public class PeruCornershopwongCrawler extends MexicoCornershopCrawlerRanking {

   public PeruCornershopwongCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return "2913";
   }

   @Override
   protected String getProductStoreId() {
      return "841";
   }
}
