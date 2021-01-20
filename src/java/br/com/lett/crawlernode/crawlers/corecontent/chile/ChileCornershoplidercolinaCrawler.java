package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class ChileCornershoplidercolinaCrawler extends CornershopCrawler {

   public static final String STORE_ID = "483";
   public static final String SELLER_FULL_NAME = "Cornershop Lider Colina";

   public ChileCornershoplidercolinaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getSellerName() {
      return SELLER_FULL_NAME;
   }
}
