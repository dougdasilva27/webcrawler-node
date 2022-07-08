package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawler;

public class ChileFalabellaCrawler extends FalabellaCrawler {
   
   public ChileFalabellaCrawler(Session session) {
      super(session);
   }

   @Override
   protected boolean isAllow3pSeller() {
      return session.getOptions().optBoolean("allow_3p_seller", true);
   }
   @Override
   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }
   @Override
   protected String getApiCode() {
      return session.getOptions().optString("api_code");
   }
   @Override
   protected String getSellerName() {
      return session.getOptions().optString("seller_name");
   }
}
