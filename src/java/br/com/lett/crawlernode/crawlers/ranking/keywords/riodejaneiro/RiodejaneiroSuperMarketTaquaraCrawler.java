package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermarketCrawler;

public class RiodejaneiroSuperMarketTaquaraCrawler extends SupermarketCrawler {

   public static final String STORE_ID = "torre-taquara";

   public RiodejaneiroSuperMarketTaquaraCrawler(Session session) {
      super(session);
   }

   @Override
   public String getStore() {
      return STORE_ID;
   }
}

