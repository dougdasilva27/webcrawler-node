package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class BrasilPrezunicCrawler extends Vipcommerce {

   private static final String HOME_PAGE = "https://www.delivery.prezunic.com.br/";
   private static final String SELLER_FULL_NAME = "Prezunic";
   private static final String DOMAIN = "delivery.prezunic.com.br";
   private static final String LOCATE_CODE = "1";

   public BrasilPrezunicCrawler(Session session) {
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
