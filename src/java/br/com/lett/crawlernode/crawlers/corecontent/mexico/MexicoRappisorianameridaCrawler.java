package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappisorianameridaCrawler extends MexicoRappiCrawler {

   public MexicoRappisorianameridaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "942000513";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
