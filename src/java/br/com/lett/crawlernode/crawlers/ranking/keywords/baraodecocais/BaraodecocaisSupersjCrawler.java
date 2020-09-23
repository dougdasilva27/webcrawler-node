package br.com.lett.crawlernode.crawlers.ranking.keywords.baraodecocais;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupersjCrawler;

public class BaraodecocaisSupersjCrawler extends SupersjCrawler {

   private static final String LOCATION_ID = "8";

   public BaraodecocaisSupersjCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationId(){
      return LOCATION_ID;
   }
}
