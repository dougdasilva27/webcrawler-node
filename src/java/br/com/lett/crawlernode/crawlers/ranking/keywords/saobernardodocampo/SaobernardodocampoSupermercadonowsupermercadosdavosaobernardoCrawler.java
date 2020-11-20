package br.com.lett.crawlernode.crawlers.ranking.keywords.saobernardodocampo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class SaobernardodocampoSupermercadonowsupermercadosdavosaobernardoCrawler extends SupermercadonowCrawlerRanking {

   public SaobernardodocampoSupermercadonowsupermercadosdavosaobernardoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-sao-bernardo";
   }
}
