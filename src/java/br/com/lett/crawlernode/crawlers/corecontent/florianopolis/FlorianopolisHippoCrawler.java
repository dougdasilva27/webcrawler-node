package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class FlorianopolisHippoCrawler extends Vipcommerce {
   private static final String HOME_PAGE = "https://www.hippo.com.br/";
   private static final String SELLER_FULL_NAME = "hippo";
   private static final String DOMAIN = "hippo.com.br";

   public FlorianopolisHippoCrawler(Session session) {
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
