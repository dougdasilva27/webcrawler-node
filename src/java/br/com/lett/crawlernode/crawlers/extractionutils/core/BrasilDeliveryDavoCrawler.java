package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;

public class BrasilDeliveryDavoCrawler extends  SupermercadonowCrawler {
   public BrasilDeliveryDavoCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://delivery.davo.com.br/";

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }
}
