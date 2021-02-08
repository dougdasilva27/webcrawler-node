package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawler;

public class ChileFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "www.falabella.com";
   private static final String API_CODE = "Falabella";
   private static final String SELLE_NAME = "Falabella";


   public ChileFalabellaCrawler(Session session) {
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
      return SELLE_NAME;
   }
}
