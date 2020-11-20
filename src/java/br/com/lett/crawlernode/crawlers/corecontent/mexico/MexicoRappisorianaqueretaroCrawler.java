package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappisorianaqueretaroCrawler extends MexicoRappiCrawler {

   public MexicoRappisorianaqueretaroCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "160194978";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
