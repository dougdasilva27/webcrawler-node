package br.com.lett.crawlernode.crawlers.ranking.keywords.itapema;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.KochCrawlerRanking;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class ItapemaKochCrawler extends KochCrawlerRanking {

   protected String storeId = "9";

   public ItapemaKochCrawler(Session session) {
      super(session);
      super.setStoreId(storeId);
   }


}
