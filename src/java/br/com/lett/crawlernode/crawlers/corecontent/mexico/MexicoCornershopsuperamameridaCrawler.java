package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershopsuperamameridaCrawler extends CornershopCrawler {

   public MexicoCornershopsuperamameridaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1918";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}