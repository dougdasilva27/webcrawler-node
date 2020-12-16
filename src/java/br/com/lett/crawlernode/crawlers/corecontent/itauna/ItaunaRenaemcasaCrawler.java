package br.com.lett.crawlernode.crawlers.corecontent.itauna;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class ItaunaRenaemcasaCrawler extends Vipcommerce {

   private static final String SELLER_FULL_NAME = "rena em casa";
   private static final String HOME_PAGE = "https://www.renaemcasa.com.br/";
   private static final String DOMAIN = "renaemcasa.com.br";

   public ItaunaRenaemcasaCrawler (Session session){
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
