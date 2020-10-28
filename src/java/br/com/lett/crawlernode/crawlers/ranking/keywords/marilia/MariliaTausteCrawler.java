package br.com.lett.crawlernode.crawlers.ranking.keywords.marilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.TausteCrawler;

public class MariliaTausteCrawler extends TausteCrawler {

   private static final String LOCATION = "1";

   public MariliaTausteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}