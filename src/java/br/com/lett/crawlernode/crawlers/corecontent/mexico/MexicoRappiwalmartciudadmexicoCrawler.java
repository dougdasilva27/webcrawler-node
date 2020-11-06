package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappiwalmartciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappiwalmartciudadmexicoCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "990007483";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
