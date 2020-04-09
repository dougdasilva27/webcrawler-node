package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermercadonowCrawlerRanking;

public class SaopauloSupermercadonowsupermercadoslopesitaimpaulistaCrawler extends SupermercadonowCrawlerRanking {

   public SaopauloSupermercadonowsupermercadoslopesitaimpaulistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-itaim-paulista";
   }
}
