package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilMartinsatacadoCrawler extends BrasilMartinsCrawler {

   public BrasilMartinsatacadoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "Nestle2020";
   }

   @Override
   protected String getLogin() {
      return "thais.araujo1@br.nestle.com";
   }
}


