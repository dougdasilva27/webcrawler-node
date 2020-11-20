package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappiwalmartsantafeCrawler extends MexicoRappiCrawler {

   public MexicoRappiwalmartsantafeCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "990006038";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
