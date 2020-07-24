package br.com.lett.crawlernode.crawlers.ranking.keywords.indaial;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilMinhacooper;

public class IndaialMinhacooperCrawler extends BrasilMinhacooper {

   private static final String store_name = "centro-indaial";

   public IndaialMinhacooperCrawler(Session session) {
      super(session);
      super.setStore_name(store_name);
   }

}
