package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;

public class RibeiraopretoSavegnagolojaoitoCrawler extends SavegnagoRanking {

   private static final String STORE_ID = "2";

   public RibeiraopretoSavegnagolojaoitoCrawler(Session session) {
      super(session);
   }

   @Override
   public String getStoreId() {
      return STORE_ID;
   }
}
