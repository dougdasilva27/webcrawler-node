package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilVilanova;

public class BrasilVilanovanestleCrawler extends BrasilVilanova {

   public BrasilVilanovanestleCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCnpj() {
      return "60.409.075/0127-54";
   }//60.409.075/0127-54

   @Override
   public String getPassword() {
      return "nestle2020";
   }

   @Override
   public String getSellerFullname() {
      return "vila nova - login nestle";
   }
}
