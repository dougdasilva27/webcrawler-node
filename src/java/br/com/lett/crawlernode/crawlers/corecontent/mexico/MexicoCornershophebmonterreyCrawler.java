package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershophebmonterreyCrawler extends CornershopCrawler {

   public MexicoCornershophebmonterreyCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "202";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}