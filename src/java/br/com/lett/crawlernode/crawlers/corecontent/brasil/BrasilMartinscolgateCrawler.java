package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilMartinscolgateCrawler extends BrasilMartinsCrawler{
   public BrasilMartinscolgateCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "roegelin83";
   }

   @Override
   protected String getLogin() {
      return "f_roegelin@hotmail.com";
   }
}
