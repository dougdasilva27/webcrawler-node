package br.com.lett.crawlernode.crawlers.ranking.keywords.itajai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BistekCrawler;

public class ItajaiBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "18";

   public ItajaiBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
