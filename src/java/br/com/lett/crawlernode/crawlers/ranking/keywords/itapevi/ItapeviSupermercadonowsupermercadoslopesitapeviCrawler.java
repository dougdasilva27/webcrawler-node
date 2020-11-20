package br.com.lett.crawlernode.crawlers.ranking.keywords.itapevi;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class ItapeviSupermercadonowsupermercadoslopesitapeviCrawler extends SupermercadonowCrawlerRanking {

   public ItapeviSupermercadonowsupermercadoslopesitapeviCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-itapevi";
   }
}
