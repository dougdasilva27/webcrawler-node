package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowhipermercadobergaminijacanaCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowhipermercadobergaminijacanaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "hipermercado-bergamini-jacana";
   }
}
