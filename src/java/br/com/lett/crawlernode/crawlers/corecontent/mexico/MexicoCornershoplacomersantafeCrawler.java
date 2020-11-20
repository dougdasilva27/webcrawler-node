package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershoplacomersantafeCrawler extends CornershopCrawler {

   public MexicoCornershoplacomersantafeCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "4450";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}