package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoSamsclubCrawler;

public class MexicoSamsclubsantacatarinamonterreyCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "storeId=0000006293";

   public MexicoSamsclubsantacatarinamonterreyCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}
