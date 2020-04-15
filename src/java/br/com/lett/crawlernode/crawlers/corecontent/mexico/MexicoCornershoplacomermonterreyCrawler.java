package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershoplacomermonterreyCrawler extends CornershopCrawler {

   public MexicoCornershoplacomermonterreyCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "9";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}