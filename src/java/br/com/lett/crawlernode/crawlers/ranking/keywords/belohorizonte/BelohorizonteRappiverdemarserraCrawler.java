package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class BelohorizonteRappiverdemarserraCrawler extends BrasilRappiCrawlerRanking {

   public BelohorizonteRappiverdemarserraCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900020342";
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
