package br.com.lett.crawlernode.crawlers.corecontent.sorocaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TausteCrawler;

public class SorocabaTausteCrawler extends TausteCrawler {

   private static final String LOCATION = "5";

   public SorocabaTausteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
