package br.com.lett.crawlernode.crawlers.ranking.keywords.bauru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.TausteCrawler;

public class BauruTausteCrawler extends TausteCrawler {

   private static final String LOCATION = "2";

   public BauruTausteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
