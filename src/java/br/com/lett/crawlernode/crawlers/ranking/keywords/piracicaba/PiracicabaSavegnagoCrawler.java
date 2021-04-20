package br.com.lett.crawlernode.crawlers.ranking.keywords.piracicaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;

public class PiracicabaSavegnagoCrawler extends SavegnagoRanking {

   public PiracicabaSavegnagoCrawler(Session session) {
      super(session);
   }
   public static final String STORE_ID = "14";

   @Override
   public String getStoreId() {
      return STORE_ID;
   }
}
