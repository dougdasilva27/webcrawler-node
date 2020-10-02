package br.com.lett.crawlernode.crawlers.corecontent.brusque;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BistekCrawler;

public class BrusqueBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "07";

   public BrusqueBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
