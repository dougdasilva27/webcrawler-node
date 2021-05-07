package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilVilanova;

public class BrasilVilanovakraftheinzCrawler extends BrasilVilanova {

   public BrasilVilanovakraftheinzCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCnpj() {
      return null;
   }

   @Override
   public String getPassword() {
      return null;
   }

   @Override
   public String getSellerFullname() {
      return "vila nova - login kraftheinz";
   }
}
