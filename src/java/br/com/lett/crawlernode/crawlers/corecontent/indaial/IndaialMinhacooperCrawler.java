package br.com.lett.crawlernode.crawlers.corecontent.indaial;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilMinhacooper;

public class IndaialMinhacooperCrawler extends BrasilMinhacooper {

   private static final String store_name = "centro-indaial";

   public IndaialMinhacooperCrawler(Session session) {
      super(session);
      super.setStore_name(store_name);
   }

}
