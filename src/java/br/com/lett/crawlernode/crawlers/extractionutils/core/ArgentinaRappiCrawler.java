package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;

public abstract class ArgentinaRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com";

   public ArgentinaRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected String getHomeDomain() {
      return "rappi.com.ar";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.ar/products";
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }
}
