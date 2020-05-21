package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class MexicoCornershopsuperamaciudadmexicoCrawler extends CornershopCrawler {

   public MexicoCornershopsuperamaciudadmexicoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "51";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}