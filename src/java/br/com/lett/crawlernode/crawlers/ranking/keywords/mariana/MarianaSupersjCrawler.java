package br.com.lett.crawlernode.crawlers.ranking.keywords.mariana;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupersjCrawler;

public class MarianaSupersjCrawler extends SupersjCrawler {

   private static final String LOCATION_ID = "7";

   public MarianaSupersjCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationId(){
      return LOCATION_ID;
   }
}
