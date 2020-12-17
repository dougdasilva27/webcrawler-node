package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class BrasilRossisupermercadosCrawler extends Vipcommerce {

   private static final String HOME_PAGE = "https://www.rossidelivery.com.br/";
   private static final String SELLER_FULL_NAME = "Rossi Delivery";
   private static final String DOMAIN = "rossidelivery.com.br";

   public BrasilRossisupermercadosCrawler(Session session) {
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
