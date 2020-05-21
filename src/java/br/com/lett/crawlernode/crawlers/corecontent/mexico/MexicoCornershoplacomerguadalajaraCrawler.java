package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershoplacomerguadalajaraCrawler extends CornershopCrawler {

   public MexicoCornershoplacomerguadalajaraCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "615";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}