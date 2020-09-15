package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappichedrauiciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappichedrauiciudadmexicoCrawler(Session session) {
      super(session);
   }

   private static final String STORE_ID = "990002982";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}
