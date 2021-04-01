package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;

public class BrasilSavegnagojaboticabalCrawler extends SavegnagoRanking {

   public BrasilSavegnagojaboticabalCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "13";

   @Override
   public String getStoreId() {
      return STORE_ID;
   }
}
