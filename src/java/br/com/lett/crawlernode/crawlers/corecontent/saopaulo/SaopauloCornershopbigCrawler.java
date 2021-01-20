package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class SaopauloCornershopbigCrawler extends CornershopCrawler {

   public SaopauloCornershopbigCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "5870";
   public static final String SELLER_FULL_NAME = "Cornershop Big Sao Paulo";


   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}
