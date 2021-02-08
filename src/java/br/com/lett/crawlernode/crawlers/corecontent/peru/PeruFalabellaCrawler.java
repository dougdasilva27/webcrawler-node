package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawler;

public class PeruFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "www.falabella.com.pe";
   private static final String API_CODE = "FalabellaPE";
   private static final String SELLER_NAME = "Falabella peru";


   public PeruFalabellaCrawler(Session session) {
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
