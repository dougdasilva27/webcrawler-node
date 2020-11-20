package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopsuperamasantafeCrawler extends CornershopCrawler {

   public MexicoCornershopsuperamasantafeCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "33";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}