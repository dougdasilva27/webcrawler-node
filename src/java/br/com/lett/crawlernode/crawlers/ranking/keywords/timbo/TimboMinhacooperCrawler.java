package br.com.lett.crawlernode.crawlers.ranking.keywords.timbo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilMinhacooper;

public class TimboMinhacooperCrawler extends BrasilMinhacooper {

   private static final String store_name = "centro-timbo";

   public TimboMinhacooperCrawler(Session session) {
      super(session);
      super.setStore_name(store_name);
   }

}
