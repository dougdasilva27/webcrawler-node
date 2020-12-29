package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourrosariopellegriniCrawler extends ArgentinaRappiCrawler {

   public static final String STORE_ID = "127499";
   public static final String STORE_TYPE = "carrefour";

   public ArgentinaRappicarrefourrosariopellegriniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return STORE_TYPE;
   }
}