package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilDeliveryDavoCrawler extends SupermercadonowCrawlerRanking {
   public BrasilDeliveryDavoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHost() {
      return "delivery.davo.com.br";
   }
}
