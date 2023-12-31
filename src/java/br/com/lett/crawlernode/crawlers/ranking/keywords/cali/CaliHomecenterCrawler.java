package br.com.lett.crawlernode.crawlers.ranking.keywords.cali;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.HomecenterCrawlerRanking;

public class CaliHomecenterCrawler extends HomecenterCrawlerRanking {

   public CaliHomecenterCrawler(Session session) {
      super(session);
   }
   @Override
   public String getCity() {
      return "Cali";
   }

   @Override
   public String getCityComuna() {
      return "3";
   }
}
