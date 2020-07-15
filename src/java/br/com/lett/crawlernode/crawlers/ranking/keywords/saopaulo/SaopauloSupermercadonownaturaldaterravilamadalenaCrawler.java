package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonownaturaldaterravilamadalenaCrawler extends SupermercadonowCrawlerRanking {


   public SaopauloSupermercadonownaturaldaterravilamadalenaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "natural-da-terra-vila-madalena";
   }
}