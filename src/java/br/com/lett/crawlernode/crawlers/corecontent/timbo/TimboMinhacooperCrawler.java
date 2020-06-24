package br.com.lett.crawlernode.crawlers.corecontent.timbo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilMinhacooper;

public class TimboMinhacooperCrawler extends BrasilMinhacooper {

   private static final String store_name = "centro-timbo";

   public TimboMinhacooperCrawler(Session session) {
      super(session);
      super.setStore_name(store_name);
   }

}
