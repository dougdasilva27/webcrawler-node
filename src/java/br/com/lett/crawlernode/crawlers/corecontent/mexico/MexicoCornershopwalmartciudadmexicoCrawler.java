package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopwalmartciudadmexicoCrawler extends CornershopCrawler {

   public MexicoCornershopwalmartciudadmexicoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "48";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}