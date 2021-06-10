package br.com.lett.crawlernode.crawlers.ranking.keywords.medellin;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.HomecenterCrawlerRanking;

public class MedellinHomecenterCrawler extends HomecenterCrawlerRanking {

   public MedellinHomecenterCrawler(Session session) {
      super(session);
   }
   @Override
   public String getCity() {
      return "Medellin";
   }

   @Override
   public String getCityComuna() {
      return "2";
   }
}
