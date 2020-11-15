package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilMartinsrbhealthCrawler extends BrasilMartinsCrawler {

   public BrasilMartinsrbhealthCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "Ju271200";
   }

   @Override
   protected String getLogin() {
      return "heleno.Junior@rb.com";
   }
}
