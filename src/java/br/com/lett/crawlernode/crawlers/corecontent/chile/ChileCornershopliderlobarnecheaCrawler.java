package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class ChileCornershopliderlobarnecheaCrawler extends CornershopCrawler {

   public static final String STORE_ID = "6";

   public ChileCornershopliderlobarnecheaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
