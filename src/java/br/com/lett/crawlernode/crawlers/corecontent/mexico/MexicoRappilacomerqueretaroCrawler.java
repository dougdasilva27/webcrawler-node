package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappilacomerqueretaroCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomerqueretaroCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1306706487";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
