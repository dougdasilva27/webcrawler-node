package br.com.lett.crawlernode.crawlers.ranking.keywords.itabira;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupersjCrawler;

public class ItabiraSupersjCrawler extends SupersjCrawler {

   private static final String LOCATION_ID = "6";

   public ItabiraSupersjCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationId(){
      return LOCATION_ID;
   }
}
