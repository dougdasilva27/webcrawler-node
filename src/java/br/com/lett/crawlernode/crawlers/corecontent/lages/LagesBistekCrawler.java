package br.com.lett.crawlernode.crawlers.corecontent.lages;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BistekCrawler;

public class LagesBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "05";

   public LagesBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
