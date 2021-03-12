package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoSamsclubCrawler;

public class MexicoSamsclubgonzalezgalloguadalajaraCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "storeId=0000006254";

   public MexicoSamsclubgonzalezgalloguadalajaraCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}
