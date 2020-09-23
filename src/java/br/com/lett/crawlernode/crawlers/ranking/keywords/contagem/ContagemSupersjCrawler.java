package br.com.lett.crawlernode.crawlers.ranking.keywords.contagem;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupersjCrawler;

public class ContagemSupersjCrawler extends SupersjCrawler {

   private static final String LOCATION_ID = "5";

   public ContagemSupersjCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocationId(){
      return LOCATION_ID;
   }
}
