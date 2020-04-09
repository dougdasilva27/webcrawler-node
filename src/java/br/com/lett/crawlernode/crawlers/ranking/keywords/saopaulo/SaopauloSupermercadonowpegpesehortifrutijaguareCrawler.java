package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowpegpesehortifrutijaguareCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowpegpesehortifrutijaguareCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "peg-pese-jaguare";
   }
}
