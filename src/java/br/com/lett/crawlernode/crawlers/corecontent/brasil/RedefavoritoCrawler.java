package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;
import br.com.lett.crawlernode.core.session.Session;

public class RedefavoritoCrawler extends Vipcommerce {



   private static final String HOME_PAGE = "https://www.favoritosupermercados.com.br";
   private static final String SELLER_FULL_NAME = "rede favorito";
   private static final String DOMAIN = "favoritosupermercados.com.br";

   public RedefavoritoCrawler(Session session) {
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
