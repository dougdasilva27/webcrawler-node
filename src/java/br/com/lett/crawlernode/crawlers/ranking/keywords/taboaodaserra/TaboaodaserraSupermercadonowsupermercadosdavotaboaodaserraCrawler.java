package br.com.lett.crawlernode.crawlers.ranking.keywords.taboaodaserra;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class TaboaodaserraSupermercadonowsupermercadosdavotaboaodaserraCrawler extends SupermercadonowCrawlerRanking {

   public TaboaodaserraSupermercadonowsupermercadosdavotaboaodaserraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-taboao-da-serra";
   }
}
