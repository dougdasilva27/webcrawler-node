package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class CarvalhosupershopCrawler extends Vipcommerce {
   private final String HOME_PAGE = "https://www.carvalhosupershop.com.br/";
   private final String SELLER_FULL_NAME = "carvalhosupershop";
   private final String DOMAIN = "carvalhosupershop.com.br";

   public CarvalhosupershopCrawler(Session session){
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
