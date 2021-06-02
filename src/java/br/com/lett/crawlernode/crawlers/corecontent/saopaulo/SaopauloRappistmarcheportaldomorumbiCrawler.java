package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappistmarcheportaldomorumbiCrawler extends BrasilRappiCrawler {

   public static final String STORE_ID = "900033773";

   public SaopauloRappistmarcheportaldomorumbiCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
