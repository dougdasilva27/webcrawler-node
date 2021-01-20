package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershoplacomerpueblaCrawler extends CornershopCrawler {

   public MexicoCornershoplacomerpueblaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "4430";
   public static final String SELLER_FULL_NAME = "Cornershop La Comer Puebla";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }


   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}
