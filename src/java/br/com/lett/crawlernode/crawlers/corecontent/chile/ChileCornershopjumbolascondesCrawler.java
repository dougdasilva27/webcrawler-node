package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class ChileCornershopjumbolascondesCrawler extends CornershopCrawler {

   // Av. Kennedy N9001, Las Condes
   public static final String STORE_ID = "328";
   public static final String SELLER_FULL_NAME = "Cornershop Jumbo Las Condes";

   public ChileCornershopjumbolascondesCrawler(Session session) {
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
