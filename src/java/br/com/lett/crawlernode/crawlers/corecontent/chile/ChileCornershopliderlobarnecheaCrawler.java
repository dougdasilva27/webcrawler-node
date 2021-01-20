package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class ChileCornershopliderlobarnecheaCrawler extends CornershopCrawler {

   public static final String STORE_ID = "6";
   public static final String SELLER_FULL_NAME = "Cornershop Lo Barnechea Lider";

   public ChileCornershopliderlobarnecheaCrawler(Session session) {
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
