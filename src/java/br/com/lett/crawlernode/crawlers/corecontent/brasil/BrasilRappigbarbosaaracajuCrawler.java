package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class BrasilRappigbarbosaaracajuCrawler extends BrasilRappiCrawler {

   public static final String STORE_ID = "900053930";

   public BrasilRappigbarbosaaracajuCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

}



