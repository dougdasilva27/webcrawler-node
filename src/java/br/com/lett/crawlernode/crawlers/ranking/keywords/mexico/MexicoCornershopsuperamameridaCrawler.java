package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoCornershopCrawlerRanking;

public class MexicoCornershopsuperamameridaCrawler extends MexicoCornershopCrawlerRanking {

  public static final String STORE_ID = br.com.lett.crawlernode.crawlers.corecontent.mexico.MexicoCornershopsuperamameridaCrawler.STORE_ID;
  public static final String PRODUCT_STORE_ID = "5";

   public MexicoCornershopsuperamameridaCrawler(Session session) {
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
