package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawler;

public class ColombiaFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "www.falabella.com.co";
   private static final String API_CODE = "FalabellaCO";
   private static final String SELLER_NAME = "Falabella Colombia";


   public ColombiaFalabellaCrawler(Session session) {
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
