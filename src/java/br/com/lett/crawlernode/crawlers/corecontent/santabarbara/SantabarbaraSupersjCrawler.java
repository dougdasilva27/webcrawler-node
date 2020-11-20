package br.com.lett.crawlernode.crawlers.corecontent.santabarbara;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupersjCrawler;

public class SantabarbaraSupersjCrawler extends SupersjCrawler {

   private static final String LOCATION_ID = "10";

   public SantabarbaraSupersjCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationId(){
      return LOCATION_ID;
   }

}
