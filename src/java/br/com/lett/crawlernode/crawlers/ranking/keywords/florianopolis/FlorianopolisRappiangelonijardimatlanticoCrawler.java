package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */

public class FlorianopolisRappiangelonijardimatlanticoCrawler extends BrasilRappiCrawlerRanking {

   public FlorianopolisRappiangelonijardimatlanticoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900049319";
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





