package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershopchedrauipueblaCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauipueblaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "461";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}