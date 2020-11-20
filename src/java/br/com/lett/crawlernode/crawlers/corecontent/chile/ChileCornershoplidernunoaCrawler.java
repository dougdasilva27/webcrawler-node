package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class ChileCornershoplidernunoaCrawler extends CornershopCrawler {

   public ChileCornershoplidernunoaCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "695";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
