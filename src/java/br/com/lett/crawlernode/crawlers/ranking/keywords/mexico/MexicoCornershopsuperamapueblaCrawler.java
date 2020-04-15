package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoCornershopCrawlerRanking;

public class MexicoCornershopsuperamapueblaCrawler extends MexicoCornershopCrawlerRanking {

   public static final String STORE_ID = br.com.lett.crawlernode.crawlers.corecontent.mexico.MexicoCornershopsuperamapueblaCrawler.STORE_ID;
   public static final String PRODUCT_STORE_ID = "450";

   public MexicoCornershopsuperamapueblaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getProductStoreId() {
      return PRODUCT_STORE_ID;
   }
}
