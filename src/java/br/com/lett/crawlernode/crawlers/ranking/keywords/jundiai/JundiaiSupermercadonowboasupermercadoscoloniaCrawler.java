package br.com.lett.crawlernode.crawlers.ranking.keywords.jundiai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class JundiaiSupermercadonowboasupermercadoscoloniaCrawler extends SupermercadonowCrawlerRanking {

   public JundiaiSupermercadonowboasupermercadoscoloniaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "boa-supermercados-jundiai-colonia";
   }
}
