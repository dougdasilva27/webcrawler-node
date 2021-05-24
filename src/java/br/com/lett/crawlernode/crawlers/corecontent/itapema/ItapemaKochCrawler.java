package br.com.lett.crawlernode.crawlers.corecontent.itapema;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.KochCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class ItapemaKochCrawler extends KochCrawler {

   protected  String storeId = "9";

   public ItapemaKochCrawler(Session session) {
      super(session);
      super.setStoreId(storeId);
   }



}
