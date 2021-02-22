package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class RiodejaneiroRappizonasullaranjeirasCrawler extends BrasilRappiCrawlerRanking {

   public RiodejaneiroRappizonasullaranjeirasCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900141059";
   public static final String STORE_TYPE = "hiper";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return STORE_TYPE;
   }


}
