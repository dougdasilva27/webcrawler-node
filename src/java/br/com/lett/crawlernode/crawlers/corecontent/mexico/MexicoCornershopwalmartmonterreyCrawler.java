package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershopwalmartmonterreyCrawler extends CornershopCrawler {

   public MexicoCornershopwalmartmonterreyCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "6";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}