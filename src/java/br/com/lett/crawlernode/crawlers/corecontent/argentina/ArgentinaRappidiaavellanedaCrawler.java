package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiaavellanedaCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "California 2098, C1276 CABA, Argentina";
   public static final String STORE_ID = "135504";

   public ArgentinaRappidiaavellanedaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
