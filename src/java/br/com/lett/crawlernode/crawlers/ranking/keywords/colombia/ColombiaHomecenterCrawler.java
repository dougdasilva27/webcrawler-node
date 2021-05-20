package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.HomecenterCrawlerRanking;

public class ColombiaHomecenterCrawler extends HomecenterCrawlerRanking {

   public ColombiaHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCity() {
      return null;
   }

   @Override
   public String getCityComuna() {
      return null;
   }
}
