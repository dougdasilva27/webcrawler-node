package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ArgentinaCarrefoursuper;

import static br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaCarrefoursuperrincondemilbergCrawler.TOKEN;

public class ArgentinaCarrefoursuperrincondemilbergCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursuperrincondemilbergCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return TOKEN;
   }

}
