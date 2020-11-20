package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopchedrauiciudadmexicoCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauiciudadmexicoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "428";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}