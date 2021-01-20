package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopchedrauiqueretaroCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauiqueretaroCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "404";
   public static final String SELLER_FULL_NAME = "Cornershop Chedraui Queretaro";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}
