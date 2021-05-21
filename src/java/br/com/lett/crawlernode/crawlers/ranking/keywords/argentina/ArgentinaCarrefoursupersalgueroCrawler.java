package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ArgentinaCarrefoursuper;

import static br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaCarrefoursupersalgueroCrawler.TOKEN;

public class ArgentinaCarrefoursupersalgueroCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursupersalgueroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return TOKEN;
   }

}
