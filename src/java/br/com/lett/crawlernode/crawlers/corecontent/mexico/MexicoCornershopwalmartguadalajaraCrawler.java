package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershopwalmartguadalajaraCrawler extends CornershopCrawler {

   public MexicoCornershopwalmartguadalajaraCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "365";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

}
