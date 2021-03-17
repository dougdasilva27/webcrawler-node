package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoSamsclubCrawler;

public class MexicoSamsclubportacentroCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "storeId=0000006295";

   public MexicoSamsclubportacentroCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}
