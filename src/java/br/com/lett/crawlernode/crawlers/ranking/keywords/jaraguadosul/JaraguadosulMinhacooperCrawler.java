package br.com.lett.crawlernode.crawlers.ranking.keywords.jaraguadosul;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilMinhacooper;

public class JaraguadosulMinhacooperCrawler extends BrasilMinhacooper {

   private static final String store_name = "v.nova-jaragua";

   public JaraguadosulMinhacooperCrawler(Session session) {
      super(session);
      super.setStore_name(store_name);
   }


}

