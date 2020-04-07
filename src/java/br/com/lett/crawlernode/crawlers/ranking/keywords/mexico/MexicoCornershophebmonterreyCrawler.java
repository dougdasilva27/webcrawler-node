package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoCornershopCrawler;

public class MexicoCornershophebmonterreyCrawler extends MexicoCornershopCrawler {

   public static final String STORE_ID = br.com.lett.crawlernode.crawlers.corecontent.mexico.MexicoCornershophebmonterreyCrawler.STORE_ID;
   public static final String PRODUCT_STORE_ID = "25";

   public MexicoCornershophebmonterreyCrawler(Session session) {
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
