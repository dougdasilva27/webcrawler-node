package br.com.lett.crawlernode.crawlers.ranking.keywords.criciuma;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BistekCrawler;

public class CriciumaBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "10";

   public CriciumaBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
