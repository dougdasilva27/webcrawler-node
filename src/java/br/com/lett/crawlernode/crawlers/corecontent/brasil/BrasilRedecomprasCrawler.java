package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;
import br.com.lett.crawlernode.core.session.Session;

public class BrasilRedecomprasCrawler extends Vipcommerce {


   public BrasilRedecomprasCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.redecomprasdelivery.com.br/";
   }

   @Override
   protected String getSellerFullName() {
      return "rede compras";
   }

   @Override
   protected String getDomain() {
      return "redecomprasdelivery.com.br";
   }
}
