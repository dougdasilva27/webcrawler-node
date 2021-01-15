package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;

public class BelohorizonteVerdemarsionCrawler extends Vipcommerce {

   public BelohorizonteVerdemarsionCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Verdemar MG";
   private static final String HOME_PAGE = "https://www.verdemaratevoce.com.br/";
   private static final String DOMAIN = "verdemaratevoce.com.br";
   private static final String LOCATE_CODE = "1";


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

