package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappisorianapueblaCrawler extends MexicoRappiCrawler {

   public MexicoRappisorianapueblaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "942000512";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
