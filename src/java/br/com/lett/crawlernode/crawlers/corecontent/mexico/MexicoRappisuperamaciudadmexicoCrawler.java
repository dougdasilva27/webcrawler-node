package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappisuperamaciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappisuperamaciudadmexicoCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1306702047";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}