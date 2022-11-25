package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermercadonowCrawlerRanking;

public class BrasilDeliverymakroCrawler extends SupermercadonowCrawlerRanking {
   public BrasilDeliverymakroCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected String getHost() {
      return "delivery.makro.com.br";
   }
}
