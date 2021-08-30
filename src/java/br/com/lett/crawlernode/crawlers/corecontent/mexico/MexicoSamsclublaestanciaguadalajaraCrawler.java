package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoSamsclubCrawler;

public class MexicoSamsclublaestanciaguadalajaraCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "0000006395";

   public MexicoSamsclublaestanciaguadalajaraCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}
