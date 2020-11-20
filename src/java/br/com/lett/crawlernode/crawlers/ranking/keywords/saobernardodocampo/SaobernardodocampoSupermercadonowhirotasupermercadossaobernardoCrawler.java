package br.com.lett.crawlernode.crawlers.ranking.keywords.saobernardodocampo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaobernardodocampoSupermercadonowhirotasupermercadossaobernardoCrawler extends SupermercadonowCrawlerRanking {

   public SaobernardodocampoSupermercadonowhirotasupermercadossaobernardoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-sao-bernardo";
   }
}
