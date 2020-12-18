package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class SaopauloVillarrealonlineCrawler extends Vipcommerce {
   private static final String HOME_PAGE = "https://www.villarrealonline.com.br/";
   private static final String SELLER_FULL_NAME = "Villarreal Supermercado";
   private static final String DOMAIN = "villarrealonline.com.br";
   private static final String LOCATE_CODE = "4";

   public SaopauloVillarrealonlineCrawler(Session session) {
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
