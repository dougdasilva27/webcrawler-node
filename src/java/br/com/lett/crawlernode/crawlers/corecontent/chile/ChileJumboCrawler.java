package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;

public class ChileJumboCrawler extends br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler {

   public ChileJumboCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(ChileJumboCrawler.HOME_PAGE));
   }
}
