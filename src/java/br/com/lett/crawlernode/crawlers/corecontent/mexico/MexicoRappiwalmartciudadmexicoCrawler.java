package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappiwalmartciudadmexicoCrawler extends MexicoRappiCrawler {

   public MexicoRappiwalmartciudadmexicoCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   public static final String STORE_ID = "1923291869";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
