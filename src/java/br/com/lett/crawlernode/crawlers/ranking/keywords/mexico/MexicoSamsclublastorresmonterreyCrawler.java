package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoSamsclubCrawler;

public class MexicoSamsclublastorresmonterreyCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "storeId=0000006275";

   public MexicoSamsclublastorresmonterreyCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}
