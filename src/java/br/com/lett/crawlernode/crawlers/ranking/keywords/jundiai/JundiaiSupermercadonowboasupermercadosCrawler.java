package br.com.lett.crawlernode.crawlers.ranking.keywords.jundiai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class JundiaiSupermercadonowboasupermercadosCrawler extends SupermercadonowCrawlerRanking {

   public JundiaiSupermercadonowboasupermercadosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "boa-supermercados-jundiai-quarto-centenario";
   }
}
