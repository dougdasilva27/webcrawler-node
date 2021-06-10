package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ArgentinaCarrefoursuper;

import static br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaCarrefoursupervicentelopezCrawler.TOKEN;

public class ArgentinaCarrefoursupervicentelopezCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursupervicentelopezCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return TOKEN;
   }

}
