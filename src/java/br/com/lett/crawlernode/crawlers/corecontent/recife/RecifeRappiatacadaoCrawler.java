package br.com.lett.crawlernode.crawlers.corecontent.recife;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;


public class RecifeRappiatacadaoCrawler extends BrasilRappiCrawler {

   public static final String STORE_ID = "900159163";

   public RecifeRappiatacadaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }


}
