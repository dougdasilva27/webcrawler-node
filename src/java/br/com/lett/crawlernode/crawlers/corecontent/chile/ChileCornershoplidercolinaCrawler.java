package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class ChileCornershoplidercolinaCrawler extends CornershopCrawler {

   public static final String STORE_ID = "483";

   public ChileCornershoplidercolinaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
