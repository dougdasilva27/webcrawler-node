package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;
import br.com.lett.crawlernode.core.session.Session;

public class DalbendeliveryCrawler extends Vipcommerce {

   private static final String HOME_PAGE = "https://www.dalbendelivery.com.br/";
   private static final String SELLER_FULL_NAME = "Dalben Delivery";
   private static final String DOMAIN = "dalbendelivery.com.br";

   public DalbendeliveryCrawler(Session session) {
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
}
