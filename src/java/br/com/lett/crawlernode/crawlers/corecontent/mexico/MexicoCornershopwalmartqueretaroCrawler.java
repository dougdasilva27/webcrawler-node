package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopwalmartqueretaroCrawler extends CornershopCrawler {

   public MexicoCornershopwalmartqueretaroCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "395";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}