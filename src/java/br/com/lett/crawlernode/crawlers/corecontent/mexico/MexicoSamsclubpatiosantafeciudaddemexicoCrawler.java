package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoSamsclubCrawler;

public class MexicoSamsclubpatiosantafeciudaddemexicoCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "0000004989";

   public MexicoSamsclubpatiosantafeciudaddemexicoCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}

