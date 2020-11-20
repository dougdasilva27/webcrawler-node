package br.com.lett.crawlernode.crawlers.ranking.keywords.blumenal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilMinhacooper;

public class BlumenalMinhacooperCrawler extends BrasilMinhacooper {

   private static final String store_name = "a.verde-bnu";

   public BlumenalMinhacooperCrawler(Session session) {
      super(session);
      super.setStore_name(store_name);
   }

}
