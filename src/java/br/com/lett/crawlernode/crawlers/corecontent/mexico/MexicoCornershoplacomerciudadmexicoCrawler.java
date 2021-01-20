package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershoplacomerciudadmexicoCrawler extends CornershopCrawler {

   public MexicoCornershoplacomerciudadmexicoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "4472";
   public static final String SELLER_FULL_NAME = "Cornershop La Comer Ciudad Del Mexico";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}
