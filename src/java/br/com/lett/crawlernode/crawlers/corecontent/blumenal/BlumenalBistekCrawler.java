package br.com.lett.crawlernode.crawlers.corecontent.blumenal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BistekCrawler;

public class BlumenalBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "17";

   public BlumenalBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
