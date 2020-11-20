package br.com.lett.crawlernode.crawlers.corecontent.jundiai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TausteCrawler;

public class JundiaiTausteCrawler extends TausteCrawler {

   private static final String LOCATION = "4";

   public JundiaiTausteCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }
}
