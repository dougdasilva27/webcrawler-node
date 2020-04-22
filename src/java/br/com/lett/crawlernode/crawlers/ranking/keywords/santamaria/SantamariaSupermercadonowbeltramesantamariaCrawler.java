package br.com.lett.crawlernode.crawlers.ranking.keywords.santamaria;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SantamariaSupermercadonowbeltramesantamariaCrawler extends SupermercadonowCrawlerRanking {

   public SantamariaSupermercadonowbeltramesantamariaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "beltrame-supermercados-santa-maria";
   }
}
