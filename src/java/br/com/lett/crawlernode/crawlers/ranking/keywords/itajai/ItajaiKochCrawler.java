package br.com.lett.crawlernode.crawlers.ranking.keywords.itajai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.KochCrawlerRanking;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class ItajaiKochCrawler extends KochCrawlerRanking {

   protected  String storeId = "5";

   public ItajaiKochCrawler(Session session) {
      super(session);
      super.setStoreId(storeId);
   }


}
