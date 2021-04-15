package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappihebmonterreyCrawler extends MexicoRappiCrawler {

   public MexicoRappihebmonterreyCrawler(Session session) {
      super(session);
      newUnification = true;
   }

    public static final String STORE_ID = "990004993";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
