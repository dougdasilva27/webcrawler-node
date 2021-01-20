package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopchedrauiguadalajaraCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauiguadalajaraCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "165";
   public static final String SELLER_FULL_NAME = "Cornershop Chedraui Guadalajara";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}
