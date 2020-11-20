package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappisuperamasantafeCrawler extends MexicoRappiCrawler {

   public MexicoRappisuperamasantafeCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "3881";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
