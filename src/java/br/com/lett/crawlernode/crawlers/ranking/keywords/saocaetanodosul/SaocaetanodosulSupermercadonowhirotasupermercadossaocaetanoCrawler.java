package br.com.lett.crawlernode.crawlers.ranking.keywords.saocaetanodosul;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaocaetanodosulSupermercadonowhirotasupermercadossaocaetanoCrawler extends SupermercadonowCrawlerRanking {

   public SaocaetanodosulSupermercadonowhirotasupermercadossaocaetanoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-sao-caetano";
   }
}
