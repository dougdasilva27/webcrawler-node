package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershopchedrauiqueretaroCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauiqueretaroCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "404";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}