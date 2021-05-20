package br.com.lett.crawlernode.crawlers.ranking.keywords.barranquilla;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.HomecenterCrawlerRanking;

public class BarranquillaHomecenterCrawler extends HomecenterCrawlerRanking {
   public BarranquillaHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCity() {
      return "Barranquilla";
   }

   @Override
   public String getCityComuna() {
      return "4";
   }
}
