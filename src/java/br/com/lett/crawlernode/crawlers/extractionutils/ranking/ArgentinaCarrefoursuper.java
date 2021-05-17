package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;

public abstract class ArgentinaCarrefoursuper extends CarrefourCrawler {

   public ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   protected String getHomePage() {
      return "https://supermercado.carrefour.com.ar/";
   }
}
