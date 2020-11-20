package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershoplacomerqueretaroCrawler extends CornershopCrawler {

   public MexicoCornershoplacomerqueretaroCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "4437";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}