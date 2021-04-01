package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BistekCrawler;

public class FlorianopolisBistekCrawler extends BistekCrawler {

   private static final String LOCATION = ""; //default locale

   public FlorianopolisBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}


