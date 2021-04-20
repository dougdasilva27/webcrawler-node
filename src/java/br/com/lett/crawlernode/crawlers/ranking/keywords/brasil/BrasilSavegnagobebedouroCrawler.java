package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;

public class BrasilSavegnagobebedouroCrawler extends SavegnagoRanking {
   public BrasilSavegnagobebedouroCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "9";

   @Override
   public String getStoreId() {
      return STORE_ID;
   }
}
