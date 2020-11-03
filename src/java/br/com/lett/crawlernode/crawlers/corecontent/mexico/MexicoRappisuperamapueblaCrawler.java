package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappisuperamapueblaCrawler extends MexicoRappiCrawler {

   public MexicoRappisuperamapueblaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "3894";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
