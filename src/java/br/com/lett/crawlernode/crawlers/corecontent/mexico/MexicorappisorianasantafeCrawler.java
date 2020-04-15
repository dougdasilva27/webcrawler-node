package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicorappisorianasantafeCrawler extends MexicoRappiCrawler {

   public MexicorappisorianasantafeCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1306718321";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}