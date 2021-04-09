package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermarketCrawler;

public class RiodejaneiroSupermarketprefeitosalessaCrawler extends SupermarketCrawler {

   public static final String STORE_ID = "torre-e-cia-supermercados";

   public RiodejaneiroSupermarketprefeitosalessaCrawler(Session session) {
      super(session);
   }

   @Override
   public String getStore() {
      return STORE_ID;
   }
}
