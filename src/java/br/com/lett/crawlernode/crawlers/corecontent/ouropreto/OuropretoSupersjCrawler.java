package br.com.lett.crawlernode.crawlers.corecontent.ouropreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupersjCrawler;

public class OuropretoSupersjCrawler extends SupersjCrawler {

   private static final String LOCATION_ID = "4";

   public OuropretoSupersjCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationId(){
      return LOCATION_ID;
   }
}
