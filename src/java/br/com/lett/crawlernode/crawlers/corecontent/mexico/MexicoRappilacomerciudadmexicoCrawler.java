package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappilacomerciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomerciudadmexicoCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "130670648";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}