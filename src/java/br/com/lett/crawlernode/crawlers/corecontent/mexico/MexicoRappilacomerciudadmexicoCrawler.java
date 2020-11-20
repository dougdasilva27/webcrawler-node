package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappilacomerciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomerciudadmexicoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "990003454";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
