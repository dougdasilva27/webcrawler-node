package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;
import br.com.lett.crawlernode.core.session.Session;

public class BelohorizonteDecisaoentregaCrawler extends Vipcommerce {

   private static final String SELLER_FULL_NAME = "Decisão Atacarejo";
   private static final String HOME_PAGE= "https://www.decisaoentrega.com.br/";
   private static final String DOMAIN = "decisaoentrega.com.br";

   public BelohorizonteDecisaoentregaCrawler(Session session) {
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
