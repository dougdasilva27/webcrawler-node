package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SodimacCrawler;

public class ChileSodimacCrawler extends SodimacCrawler {
   public ChileSodimacCrawler(Session session) {
      super(session);
   }

   @Override
   public String getBaseUrl() {
      return "https://www.sodimac.cl/sodimac-cl/";
   }

   @Override
   public char getPriceFormat() {
      return '.';
   }
}
