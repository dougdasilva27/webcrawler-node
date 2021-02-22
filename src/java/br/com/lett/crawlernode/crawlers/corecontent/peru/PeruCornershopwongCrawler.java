package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class PeruCornershopwongCrawler extends CornershopCrawler {

   public PeruCornershopwongCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "2913";
   public static final String SELLER_FULL_NAME = "Cornershop Wong";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}


