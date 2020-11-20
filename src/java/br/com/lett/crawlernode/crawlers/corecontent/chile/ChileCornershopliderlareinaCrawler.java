package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class ChileCornershopliderlareinaCrawler extends CornershopCrawler {

   public static final String STORE_ID = "625";

   public ChileCornershopliderlareinaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
