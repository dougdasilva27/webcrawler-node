package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawler;

public class ArgentinaFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "www.falabella.com.ar";
   private static final String API_CODE = "FalabellaAR";
   private static final String SELLER_NAME = "Falabella Argentina";


   public ArgentinaFalabellaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getApiCode() {
      return API_CODE;
   }

   @Override
   protected String getSellerName() {
      return SELLER_NAME;
   }
}
