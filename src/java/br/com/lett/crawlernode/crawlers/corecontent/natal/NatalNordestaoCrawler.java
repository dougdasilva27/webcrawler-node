package br.com.lett.crawlernode.crawlers.corecontent.natal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class NatalNordestaoCrawler extends Vipcommerce {

   private static final String SELLER_FULL_NAME = "Nordestao";
   private static final String HOME_PAGE = "https://www.cliqueretire.nordestao.com.br/";
   private static final String DOMAIN = "cliqueretire.nordestao.com.br";
   private static final String LOCALE_CODE = "2";


   public NatalNordestaoCrawler (Session session){
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
      return LOCALE_CODE;
   }
}
