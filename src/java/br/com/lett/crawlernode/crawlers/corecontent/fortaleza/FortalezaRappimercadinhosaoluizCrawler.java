package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class FortalezaRappimercadinhosaoluizCrawler extends BrasilRappiCrawler {

   public static final String STORE_ID = "900022515";

   public FortalezaRappimercadinhosaoluizCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

}
