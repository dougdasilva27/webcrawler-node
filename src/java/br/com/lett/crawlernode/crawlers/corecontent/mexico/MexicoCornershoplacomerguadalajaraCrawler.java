package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershoplacomerguadalajaraCrawler extends CornershopCrawler {

   public MexicoCornershoplacomerguadalajaraCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "615";
   public static final String SELLER_FULL_NAME = "Cornershop City Market La Comer Guadalajara";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }

}
