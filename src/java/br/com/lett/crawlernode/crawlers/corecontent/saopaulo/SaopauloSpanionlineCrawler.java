package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class SaopauloSpanionlineCrawler extends Vipcommerce {
   private static final String HOME_PAGE = "https://www.spanionline.com.br/";
   private static final String SELLER_FULL_NAME = "Spani online";
   private static final String DOMAIN = "spanionline.com.br";
   private static final String LOCATE_CODE = "10";

   public SaopauloSpanionlineCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getSellerFullName() {
      return SELLER_FULL_NAME;
   }

   @Override
   protected String getDomain() {
      return DOMAIN;
   }

   @Override
   protected String getLocateCode() {
      return LOCATE_CODE;
   }
}
